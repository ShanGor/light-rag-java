package cn.gzten.rag.llm.impl;

import cn.gzten.rag.data.pojo.LlmStreamData;
import cn.gzten.rag.llm.LlmCompletionFunc;
import cn.gzten.rag.service.HttpService;
import cn.gzten.rag.util.LightRagUtils;
import com.fasterxml.jackson.annotation.JsonAlias;
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
    public Flux<ServerSentEvent<String>> completeStream(List<CompletionMessage> messages, Options options) {
        options.setStream(true);
        var requestId = UUID.randomUUID().toString();
        return httpService.postSeverSentEvent(url, null, Map.of("model", model,
                "messages", messages,
                "stream", true,
                "options", options), true).map(sse -> {
                    var data = sse.data();
                    if (StringUtils.isBlank(data))
                        return """
                                {"id":"%s","done":false}""".formatted(requestId);
                    var obj = LightRagUtils.jsonToObject(data, OllamaStreamResult.class);
                    var res = new LlmStreamData();
                    res.setId(requestId);
                    res.setModel(obj.getModel());
                    res.setCreatedAt(obj.getCreatedAt());
                    res.setDone(obj.isDone());
                    var choice = new LlmStreamData.Choice();
                    choice.setIndex(0);
                    choice.setMessage(obj.getMessage());
                    choice.setFinishReason(obj.getDoneReason());
                    res.setChoices(List.of(choice));
                    if (obj.evalDuration != null) {
                        var usage = new LlmStreamData.Usage();
                        usage.setCompletionTokens(obj.getEvalCount().intValue());
                        usage.setTotalTokens(obj.getEvalCount().intValue() + obj.getPromptEvalCount().intValue());
                        usage.setPromptTokens(obj.getPromptEvalCount().intValue());
                        res.setUsage(usage);
                    }
                    return LightRagUtils.objectToJsonSnake(res);
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
        @JsonAlias("created_at")
        private String createdAt;
        @JsonAlias("total_duration")
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

    @Data
    public static class OllamaStreamResult {
        private String model;
        @JsonAlias("created_at")
        private String createdAt;
        private CompletionMessage message;
        private boolean done;
        @JsonAlias("done_reason")
        private String doneReason;
        @JsonAlias("total_duration")
        private Long totalDuration;
        @JsonAlias("load_duration")
        private Long loadDuration;
        @JsonAlias("prompt_eval_count")
        private Long promptEvalCount;
        @JsonAlias("prompt_eval_duration")
        private Long promptEvalDuration;
        @JsonAlias("eval_count")
        private Long evalCount;
        @JsonAlias("eval_duration")
        private Long evalDuration;
    }
}
