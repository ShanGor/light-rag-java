package cn.gzten.rag.data.pojo;

import cn.gzten.rag.llm.LlmCompletionFunc;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * Formalized data for LLM stream. To avoid different LLM difficulties.
 */
@Data
public class LlmStreamData {
    private String id;
    private String model;
    @JsonAlias("created_at")
    private String createdAt;

    private List<Choice> choices;

    private Usage usage;
    private boolean done;

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
        private int promptTokens;
        @JsonAlias("completion_tokens")
        private int completionTokens;
        @JsonAlias("total_tokens")
        private int totalTokens;
    }


}
