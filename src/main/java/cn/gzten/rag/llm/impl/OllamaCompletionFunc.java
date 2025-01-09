package cn.gzten.rag.llm.impl;

import cn.gzten.rag.llm.LlmCompletionFunc;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@Service
@ConditionalOnProperty(value = "rag.llm.completion.provider", havingValue = "ollama")
public class OllamaCompletionFunc implements LlmCompletionFunc {
    @Value("${rag.llm.completion.model}")
    private String model;
    @Value("${rag.llm.embedding.url:http://localhost:11434/api/chat}")
    private URI url;

    private Options options = new Options();

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    HttpService httpService;

    @Override
    public OllamaResult complete(List<CompletionMessage> messages) {
        return complete(messages, options);
    }

    @Override
    public OllamaResult complete(List<CompletionMessage> messages, Options options) {
        return httpService.llmComplete(Map.of("model", model,
                "messages", messages,
                "stream", options.isStream(),
                "options", options), OllamaResult.class);
    }

    @Override
    public OllamaResult complete(String prompt, List<CompletionMessage> historyMessages, Options options) {
        var messages = new LinkedList<>(historyMessages);
        var message = new CompletionMessage();
        message.setContent(prompt);
        message.setRole("user");
        messages.add(message);
        return complete(messages, options);
    }

    @Override
    public OllamaResult complete(String prompt, List<CompletionMessage> historyMessages) {
        return complete(prompt, historyMessages, options);
    }

    /**
     * {
     *   "model": "llama3.2",
     *   "created_at": "2023-12-12T14:13:43.416799Z",
     *   "message": {
     *     "role": "assistant",
     *     "content": "Hello! How are you today?"
     *   },
     *   "done": true,
     *   "total_duration": 5191566416,
     *   "load_duration": 2154458,
     *   "prompt_eval_count": 26,
     *   "prompt_eval_duration": 383809000,
     *   "eval_count": 298,
     *   "eval_duration": 4799921000
     * }
     */

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class OllamaResult extends CompletionResult{
        @JsonAlias("created_at")
        private String createdAt;
        private long totalDuration;
        @JsonAlias("load_duration")
        private long loadDuration;
        @JsonAlias("prompt_eval_count")
        private long promptEvalCount;
        @JsonAlias("prompt_eval_duration")
        private long promptEvalDuration;
        @JsonAlias("eval_count")
        private long evalCount;
        @JsonAlias("eval_duration")
        private long evalDuration;
    }
}
