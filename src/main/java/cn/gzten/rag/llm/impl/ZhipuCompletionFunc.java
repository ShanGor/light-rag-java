package cn.gzten.rag.llm.impl;

import cn.gzten.rag.data.pojo.GPTKeywordExtractionFormat;
import cn.gzten.rag.data.pojo.ZhipuResult;
import cn.gzten.rag.llm.LlmCompletionFunc;
import cn.gzten.rag.service.HttpService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Service
@ConditionalOnProperty(value = "rag.llm.completion.provider", havingValue = "zhipu")
public class ZhipuCompletionFunc extends LlmCompletionFunc {
    @Value("${rag.llm.completion.model}")
    private String model;
    @Value("${rag.llm.completion.url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private URI url;
    private Map<String, String> headers;
    @Resource
    private ObjectMapper objectMapper;

    public ZhipuCompletionFunc(@Value("${rag.llm.completion.api-key}") String apiKey) {
        this.headers = Map.of("Authorization", "Bearer " + apiKey,
                "Content-Type", "application/json");
    }

    @Resource
    HttpService httpService;

    @Override
    public ZhipuResult complete(List<CompletionMessage> messages, Options options) {
        var result = httpService.post(url, headers,
                Map.of("model", model,
                        "messages", messages,
                        "temperature", options.getTemperature(),
                        "stream", options.isStream()),
                ZhipuResult.class);
        var messageBuilder = new StringBuilder();
        for (var choice : result.getChoices()) {
            messageBuilder.append(choice.getMessage().getContent());
        }
        result.setMessage(CompletionMessage.builder().content(messageBuilder.toString()).role("assistant").build());
        return result;
    }

    public static Pattern KW_JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*}");

    @Override
    public Object complete(LightRagRequest ragRequest) {
        if (ragRequest.isKeywordExtraction()) {
            var extraction_prompt = """
                You are a helpful assistant that extracts keywords from text.
                Please analyze the content and extract two types of keywords:
                1. High-level keywords: Important concepts and main themes
                2. Low-level keywords: Specific details and supporting elements

                Return your response in this exact JSON format:
                {
                    "high_level_keywords": ["keyword1", "keyword2"],
                    "low_level_keywords": ["keyword1", "keyword2", "keyword3"]
                }
        
                Only return the JSON, no other text.""";

            // Combine with existing system prompt if any
            String systemPrompt;
            if (StringUtils.isNotBlank(ragRequest.getSystemPrompt())) {
                systemPrompt = "%s\n\n%s".formatted(ragRequest.getSystemPrompt(), extraction_prompt);
            } else {
                systemPrompt = extraction_prompt;
            }

            if (ragRequest.getHistoryMessages().isEmpty()) {
                ragRequest.setHistoryMessages(List.of(CompletionMessage.builder().role("system").content(systemPrompt).build()));
            } else {
                var foundSystemPrompt = false;
                for (var message : ragRequest.getHistoryMessages()) {
                    if (message.getRole().equals("system")) {
                        foundSystemPrompt = true;
                        break;
                    }
                }
                if (!foundSystemPrompt) {
                    try {
                        ragRequest.getHistoryMessages().add(0, CompletionMessage.builder().role("system").content(systemPrompt).build());
                    } catch (UnsupportedOperationException e) {
                        // If the list is immutable, create a new list and add the system prompt
                        var history = new LinkedList<CompletionMessage>();
                        history.add(CompletionMessage.builder().role("system").content(systemPrompt).build());
                        history.addAll(ragRequest.getHistoryMessages());
                        ragRequest.setHistoryMessages(history);
                    }

                }
            }

            try {
                var response = complete(ragRequest.getPrompt(), ragRequest.getHistoryMessages());
                try{
                    return objectMapper.readValue(response.getMessage().getContent(), GPTKeywordExtractionFormat.class);
                } catch (JsonProcessingException e) {
                    // If direct JSON parsing fails,try to extract JSON from text
                    var match = KW_JSON_PATTERN.matcher(response.getMessage().getContent());
                    if (match.find()) {
                        try {
                            return objectMapper.readValue(match.group(), GPTKeywordExtractionFormat.class);
                        } catch (JsonProcessingException ignored) {}
                    }
                    log.warn("Failed to parse keyword extraction response: {}", response);
                    return GPTKeywordExtractionFormat.builder()
                            .lowLevelKeywords(Collections.emptyList())
                            .highLevelKeywords(Collections.emptyList()).build();


                }

                // If all parsing fails, log warning and return empty format

            } catch (Exception e) {
                log.error("Error during keyword extraction: {}", e.getMessage());
                return GPTKeywordExtractionFormat.builder()
                        .lowLevelKeywords(Collections.emptyList())
                        .highLevelKeywords(Collections.emptyList()).build();
            }

        } else {
            // For non-keyword-extraction, just return the raw response string
            var history = new LinkedList<CompletionMessage>();
            if (!StringUtils.isBlank(ragRequest.getSystemPrompt())) {
                if (ragRequest.getHistoryMessages().isEmpty()) {
                    history.add(CompletionMessage.builder().role("system").content(ragRequest.getSystemPrompt()).build());
                } else {
                    var foundSystemPrompt = false;
                    for (var message : ragRequest.getHistoryMessages()) {
                        if (message.getRole().equals("system")) {
                            foundSystemPrompt = true;
                            break;
                        }
                    }
                    if (!foundSystemPrompt) {
                        history.add(CompletionMessage.builder().role("system").content(ragRequest.getSystemPrompt()).build());
                    }
                    history.addAll(ragRequest.getHistoryMessages());
                }
            } else {
                history.addAll(ragRequest.getHistoryMessages());
            }

            try {
                return complete(ragRequest.getPrompt(), history).getMessage().getContent();
            } catch (Exception e) {
                log.error("Error during completion: {}", e.getMessage());
                return null;
            }
        }

    }




}
