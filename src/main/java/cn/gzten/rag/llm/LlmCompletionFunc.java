package cn.gzten.rag.llm;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

public interface LlmCompletionFunc {
    String getModel();
    String getUrl();
    Options getOptions();
    CompletionResult complete(List<CompletionMessage> messages);
    CompletionResult complete(List<CompletionMessage> messages, Options options);
    CompletionResult complete(String prompt, List<CompletionMessage> historyMessages, Options options);
    CompletionResult complete(String prompt, List<CompletionMessage> historyMessages);

    @Data
    class Options {
        private float temperature = 0.2f;
        private boolean stream = false;
    }

    @Data
    class CompletionMessage {
        private String role;
        private String content;
    }

    @Data
    class CompletionResult {
        protected String model;
        protected CompletionMessage message;
        private boolean done;
    }

}
