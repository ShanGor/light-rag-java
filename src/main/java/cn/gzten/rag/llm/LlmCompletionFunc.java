package cn.gzten.rag.llm;

import lombok.Data;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Data
public abstract class LlmCompletionFunc {
    protected String model;
    protected URI url;
    protected Options options = new Options();
    public CompletionResult complete(List<CompletionMessage> messages) {
        return complete(messages, options);
    }

    public abstract CompletionResult complete(List<CompletionMessage> messages, Options options);
    public CompletionResult complete(String prompt, List<CompletionMessage> historyMessages, Options options) {
        var messages = new LinkedList<>(historyMessages);
        var message = new CompletionMessage();
        message.setContent(prompt);
        message.setRole("user");
        messages.add(message);
        return complete(messages, options);
    }
    public CompletionResult complete(String prompt, List<CompletionMessage> historyMessages) {
        return complete(prompt, historyMessages, options);
    }

    public CompletionResult complete(String prompt) {
        return complete(prompt, Collections.emptyList(), options);
    }

    @Data
    public static class Options {
        private float temperature = 0.2f;
        private boolean stream = false;
    }

    @Data
    public static class CompletionMessage {
        private String role;
        private String content;
    }

    @Data
    public static class CompletionResult {
        protected String model;
        protected CompletionMessage message;
        private boolean done;
    }

}
