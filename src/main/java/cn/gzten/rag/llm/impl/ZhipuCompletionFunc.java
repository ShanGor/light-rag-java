package cn.gzten.rag.llm.impl;

import cn.gzten.rag.llm.LlmCompletionFunc;
import com.fasterxml.jackson.annotation.JsonAlias;
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
@ConditionalOnProperty(value = "rag.llm.completion.provider", havingValue = "zhipu")
public class ZhipuCompletionFunc implements LlmCompletionFunc {
    @Value("${rag.llm.completion.model}")
    private String model;
    @Value("${rag.llm.embedding.url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private URI url;
    @Resource
    HttpService httpService;

    private Options options = new Options();

    @Override
    public ZhipuResult complete(List<CompletionMessage> messages) {
        return complete(messages, options);
    }

    @Override
    public ZhipuResult complete(List<CompletionMessage> messages, Options options) {
        return httpService.llmComplete(Map.of("model", model,
                "messages", messages,
                "temperature", options.getTemperature(),
                "stream", options.isStream()), ZhipuResult.class);
    }

    @Override
    public ZhipuResult complete(String prompt, List<CompletionMessage> historyMessages, Options options) {
        var messages = new LinkedList<>(historyMessages);
        var message = new CompletionMessage();
        message.setContent(prompt);
        message.setRole("user");
        messages.add(message);
        return complete(messages, options);
    }

    @Override
    public ZhipuResult complete(String prompt, List<CompletionMessage> historyMessages) {
        return complete(prompt, historyMessages, options);
    }

    /**
     * Ref to <a href="https://bigmodel.cn/dev/api/normal-model/glm-4">Document</a>
     * {
     *   "created": 1703487403,
     *   "id": "8239375684858666781",
     *   "model": "glm-4-plus",
     *   "request_id": "8239375684858666781",
     *   "choices": [
     *       {
     *           "finish_reason": "stop",
     *           "index": 0,
     *           "message": {
     *               "content": "以AI绘蓝图 — 智谱AI，让创新的每一刻成为可能。",
     *               "role": "assistant"
     *           }
     *       }
     *   ],
     *   "usage": {
     *       "completion_tokens": 217,
     *       "prompt_tokens": 31,
     *       "total_tokens": 248
     *   }
     * }
     */

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class ZhipuResult extends CompletionResult{
        private String id;
        @JsonAlias("request_id")
        private String requestId;
        @JsonAlias("created")
        private String createdAt;
        private List<Choice> choices;
        private Usage usage;

        @Data
        public static class Usage {
            @JsonAlias("completion_tokens")
            private int completionTokens;
            @JsonAlias("prompt_tokens")
            private int promptTokens;
            @JsonAlias("total_tokens")
            private int totalTokens;
        }

        @Data
        public static class Choice {
            @JsonAlias("finish_reason")
            private String finishReason;
            private int index;
            private CompletionMessage message;
        }
    }
}
