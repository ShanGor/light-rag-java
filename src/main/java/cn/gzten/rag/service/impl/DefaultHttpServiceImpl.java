package cn.gzten.rag.service.impl;

import cn.gzten.rag.service.HttpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static cn.gzten.rag.util.LightRagUtils.isEmptyCollection;

@Slf4j
@RequiredArgsConstructor
public class DefaultHttpServiceImpl implements HttpService {
    private final ObjectMapper objectMapper;
    private final HttpClient client = HttpClient.newHttpClient();
    private final WebClient asyncClient = WebClient.builder().build();
    @Override
    public <T> T post(URI url,
                      Map<String, String> headers,
                      Map<String, Object> requestBody,
                      Class<T> clazz) {
        try {
            var body = objectMapper.writeValueAsString(requestBody);
            var postBuilder = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .uri(url);
            if (!isEmptyCollection(headers)) {
                for (var entry : headers.entrySet()) {
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

    @Override
    public <T> Mono<T> postAsync(URI url, Map<String, String> headers,
                                 Object requestBody,
                                 Class<T> clazz) {
        var builder = asyncClient.post().uri(url);
        if (!isEmptyCollection(headers)) {
            for (var entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        return builder.bodyValue(requestBody).retrieve().bodyToMono(clazz);
    }
}
