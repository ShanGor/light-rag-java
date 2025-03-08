package cn.gzten.rag.data.pojo;

import cn.gzten.rag.llm.LlmCompletionFunc;
import lombok.Data;


import java.util.List;

@Data
public class OpenAiLlmResult {
    private String id;
    private String object;
    private long created;
    private String model;
    private String system_fingerprint;
    private List<Choice> choices;
    private Usage usage;
    @Data
    public static class Choice {
        private int index;
        private LlmCompletionFunc.CompletionMessage message;
        private Boolean logprobs;
        private String finish_reason;
    }

    @Data
    public static class Usage {
        private long prompt_tokens;
        private long completion_tokens;
        private long total_tokens;
        private CompletionTokenDetails completion_token_details;

        @Data
        public static class CompletionTokenDetails {
            private long reasoning_tokens;
            private long accepted_prediction_tokens;
            private long rejected_prediction_tokens;
        }
    }
}
