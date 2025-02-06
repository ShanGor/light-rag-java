package cn.gzten.rag.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

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
        messages.add(CompletionMessage.builder()
                .role("user")
                .content(prompt).build()
        );
        return complete(messages, options);
    }
    public CompletionResult complete(String prompt, List<CompletionMessage> historyMessages) {
        return complete(prompt, historyMessages, options);
    }

    public abstract Flux<ServerSentEvent<String>> completeStream(List<CompletionMessage> messages, Options options) ;
    public Flux<ServerSentEvent<String>> completeStream(String prompt) {
        return completeStream(prompt, Collections.emptyList(), options);
    }
    public Flux<ServerSentEvent<String>> completeStream(String prompt, List<CompletionMessage> historyMessages, Options options) {
        var messages = new LinkedList<>(historyMessages);
        messages.add(CompletionMessage.builder()
                .role("user")
                .content(prompt).build()
        );
        return completeStream(messages, options);
    }

    public CompletionResult complete(String prompt) {
        return complete(prompt, Collections.emptyList(), options);
    }

    public abstract Object complete(LightRagRequest ragRequest);

    @Data
    public static class Options {
        private float temperature = 0.2f;
        private boolean stream = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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

    @Data
    public static class LightRagRequest {
        private String prompt;
        private String systemPrompt = null;
        private List<CompletionMessage> historyMessages = Collections.emptyList();
        private boolean keywordExtraction = false;
    }

}
