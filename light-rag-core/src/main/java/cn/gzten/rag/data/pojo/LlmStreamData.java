package cn.gzten.rag.data.pojo;

import cn.gzten.rag.llm.LlmCompletionFunc;
import cn.gzten.rag.llm.impl.OllamaCompletionFunc;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Formalized data for LLM stream. To avoid different LLM difficulties.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LlmStreamData extends OllamaCompletionFunc.OllamaStreamResult {
    private String id;

    @Data
    public static class Choice {
        private int index;
        @JsonAlias("delta")
        private LlmCompletionFunc.CompletionMessage message;
        @JsonAlias("finish_reason")
        private String finishReason;

    }
    @Data
    public static class Usage {
        @JsonAlias("prompt_tokens")
        private long promptTokens;
        @JsonAlias("completion_tokens")
        private long completionTokens;
        @JsonAlias("total_tokens")
        private long totalTokens;
    }
}
