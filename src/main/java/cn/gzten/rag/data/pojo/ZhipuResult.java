package cn.gzten.rag.data.pojo;

import cn.gzten.rag.llm.LlmCompletionFunc;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Ref to <a href="https://bigmodel.cn/dev/api/normal-model/glm-4">Document</a>
 * {
 *   "choices": [
 *     {
 *       "finish_reason": "stop",
 *       "index": 0,
 *       "message": {
 *         "content": "你好👋！很高兴见到你，有什么可以帮助你的吗？",
 *         "role": "assistant"
 *       }
 *     }
 *   ],
 *   "created": 1737638605,
 *   "id": "20250123212325fbee211cb9734f41",
 *   "model": "glm-4-flashx",
 *   "request_id": "20250123212325fbee211cb9734f41",
 *   "usage": {
 *     "completion_tokens": 16,
 *     "prompt_tokens": 6,
 *     "total_tokens": 22
 *   }
 * }
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ZhipuResult extends LlmCompletionFunc.CompletionResult {
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
        private LlmCompletionFunc.CompletionMessage message;
    }
}
