package cn.gzten.rag.llm.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
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
    private final HttpClient client = HttpClient.newHttpClient();
    public <T> T post(URI url,
                      Map<String, String> headers,
                      Map<String, Object> requestBody,
                      Class<T> clazz) {
        try {
            var body = objectMapper.writeValueAsString(requestBody);
            var postBuilder = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .uri(url);
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    postBuilder.header(entry.getKey(), entry.getValue());
                }
            }
            var post = postBuilder.build();

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
