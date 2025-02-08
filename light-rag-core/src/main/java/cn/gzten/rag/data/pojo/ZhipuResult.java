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
    private long created;
    private List<LlmStreamData.Choice> choices;
    private LlmStreamData.Usage usage;
}
