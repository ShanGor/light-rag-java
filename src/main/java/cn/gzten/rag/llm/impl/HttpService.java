package cn.gzten.rag.llm.impl;

import cn.gzten.rag.llm.LlmCompletionFunc;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class HttpService {
    private final ObjectMapper objectMapper;
    public <T extends LlmCompletionFunc.CompletionResult> T llmComplete(Map<String, Object> requestBody, Class<T> clazz) {
        try (var client = HttpClient.newHttpClient()) {
            var body = objectMapper.writeValueAsString(requestBody);
            var post = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();

            var response = client.send(post, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), clazz);
            }
            throw new RuntimeException("Failed to complete the %s: %d - %s!".formatted(clazz.getName(), response.statusCode(), response.body()));
        } catch (IOException | InterruptedException e) {
            log.error("Failed to complete the {}: {}", clazz.getName(), e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
