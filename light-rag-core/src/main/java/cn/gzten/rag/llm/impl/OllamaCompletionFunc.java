package cn.gzten.rag.llm.impl;

import cn.gzten.rag.data.pojo.LlmStreamData;
import cn.gzten.rag.llm.LlmCompletionFunc;
import cn.gzten.rag.service.HttpService;
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
    public OllamaResult complete(List<CompletionMessage> messages, Options options) {
        return httpService.post(url, headers, Map.of("model", model,
                "messages", messages,
                "stream", options.isStream(),
                "options", options), OllamaResult.class);
    }

    @Override
    public Flux<ServerSentEvent<LlmStreamData>> completeStream(List<CompletionMessage> messages, Options options) {
        options.setStream(true);
        var requestId = UUID.randomUUID().toString();
        return httpService.postSeverSentEvent(url, null, Map.of("model", model,
                "messages", messages,
                "stream", true,
                "options", options), true).map(sse -> {
                    var data = sse.data();
                    if (StringUtils.isBlank(data)) {
                        var res = new LlmStreamData();
                        res.setId(requestId);
                        res.setDone(false);
                        return res;
                    }
                    var obj = LightRagUtils.jsonToObject(data, LlmStreamData.class);
                    obj.setId(requestId);
                    return obj;
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

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class OllamaResult extends CompletionResult{
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
