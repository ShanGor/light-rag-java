package cn.gzten.rag.llm.impl;

import cn.gzten.rag.data.pojo.OpenAiLlmResult;
import cn.gzten.rag.data.pojo.OpenAiLlmStreamResult;
import cn.gzten.rag.llm.LlmCompletionFunc;
import cn.gzten.rag.service.HttpService;
import cn.gzten.rag.util.DateTimeUtils;
import cn.gzten.rag.util.LightRagUtils;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Service
@ConditionalOnProperty(value = "rag.llm.completion.provider", havingValue = "ollama")
public class OllamaCompletionFunc extends LlmCompletionFunc {
    @Value("${rag.llm.completion.model}")
    private String model;
    @Value("${rag.llm.embedding.url:http://localhost:11434/api/chat}")
    private URI url;
    private Map<String, String> headers = Map.of("Content-Type", "application/json;charset=utf-8");

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    HttpService httpService;

    @Override
    public OpenAiLlmResult complete(List<CompletionMessage> messages, Options options) {
        var ollamaResult = httpService.post(url, headers, Map.of("model", model,
                "messages", messages,
                "stream", options.isStream(),
                "options", options), OllamaResult.class);
        var result = new OpenAiLlmResult();
        var usage = new OpenAiLlmResult.Usage();
        usage.setTotal_tokens(ollamaResult.getEvalCount() + ollamaResult.getPromptEvalCount());
        usage.setPrompt_tokens(ollamaResult.getPromptEvalCount());
        usage.setCompletion_tokens(ollamaResult.getEvalCount());
        result.setUsage(usage);
        var choice = new OpenAiLlmResult.Choice();
        choice.setIndex(0);
        choice.setMessage(ollamaResult.getMessage());
        choice.setFinish_reason(ollamaResult.getDoneReason());
        result.setModel(model);
        result.setCreated(DateTimeUtils.parseOllamaDateTime(ollamaResult.getCreatedAt()));
        return result;
    }

    @Override
    public Flux<ServerSentEvent<OpenAiLlmStreamResult>> completeStream(List<CompletionMessage> messages, Options options) {
        options.setStream(true);
        var requestId = UUID.randomUUID().toString();
        return httpService.postSeverSentEvent(url, null, Map.of("model", model,
                "messages", messages,
                "stream", true,
                "options", options), true).mapNotNull(sse -> {
                    var data = sse.data();
                    if (StringUtils.isBlank(data)) {
                        return null;
                    }
                    var obj = LightRagUtils.jsonToObject(data, OllamaStreamResult.class);
                    var res = new OpenAiLlmStreamResult();
                    res.setId(requestId);
                    res.setModel(obj.getModel());
                    res.setCreated(DateTimeUtils.parseOllamaDateTime(obj.getCreatedAt()));
                    if (obj.isDone()) {
                        var usage = new OpenAiLlmStreamResult.Usage();
                        usage.setTotal_tokens(obj.getEvalCount() + obj.getPromptEvalCount());
                        usage.setPrompt_tokens(obj.getPromptEvalCount().intValue());
                        usage.setCompletion_tokens(obj.getEvalCount().intValue());
                        res.setUsage(usage);
                    }
                    var choice = new OpenAiLlmStreamResult.Choice();
                    choice.setIndex(0);
                    choice.setDelta(obj.getMessage());
                    choice.setFinish_reason(obj.getDoneReason());
                    res.setChoices(List.of(choice));

                    return res;
        }).map(s -> ServerSentEvent.builder(s).id(requestId).event("llm").build());
    }

    @Override
    public Object complete(LightRagRequest ragRequest) {
        return null;
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

    @Data
    public static class OllamaResult{
        private String model;
        private CompletionMessage message;
        private boolean done;
        @JsonProperty("created_at")
        protected String createdAt;
        @JsonProperty("done_reason")
        protected String doneReason;
        @JsonProperty("total_duration")
        protected long totalDuration;
        @JsonProperty("load_duration")
        protected long loadDuration;
        @JsonProperty("prompt_eval_count")
        protected long promptEvalCount;
        @JsonProperty("prompt_eval_duration")
        protected long promptEvalDuration;
        @JsonProperty("eval_count")
        protected long evalCount;
        @JsonProperty("eval_duration")
        protected long evalDuration;
    }

    @Data
    public static class OllamaStreamResult {
        protected String model;
        @JsonProperty("created_at")
        protected String createdAt;
        protected CompletionMessage message;
        protected boolean done;
        @JsonProperty("done_reason")
        protected String doneReason;
        @JsonProperty("total_duration")
        protected Long totalDuration;
        @JsonProperty("load_duration")
        protected Long loadDuration;
        @JsonProperty("prompt_eval_count")
        protected Long promptEvalCount;
        @JsonProperty("prompt_eval_duration")
        protected Long promptEvalDuration;
        @JsonProperty("eval_count")
        protected Long evalCount;
        @JsonProperty("eval_duration")
        protected Long evalDuration;
    }
}
