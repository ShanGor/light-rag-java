package cn.gzten.rag.data.pojo;

import cn.gzten.rag.llm.LlmCompletionFunc;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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
