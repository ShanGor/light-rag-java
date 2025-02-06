package cn.gzten.rag.service.impl;

import cn.gzten.rag.service.HttpService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
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
    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<ServerSentEvent<String>>(){};
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

    @Override
    public Flux<ServerSentEvent<String>> postSeverSentEvent(URI url, Map<String, String> headers, Object requestBody, boolean raw) {
        var builder = asyncClient.post().uri(url);
        if (!isEmptyCollection(headers)) {
            for (var entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
            builder.header("Accept", "text/event-stream");
            builder.header("Cache-Control", "no-cache");
            builder.header("Connection", "keep-alive");
            builder.header("Content-Type", "text/stream-event;charset=utf-8");
        }
        if (raw) {
            return builder.bodyValue(requestBody).retrieve().bodyToFlux(String.class).map(s -> ServerSentEvent.builder(s).build());
        } else {
            return builder.bodyValue(requestBody).retrieve().bodyToFlux(SSE_TYPE);
        }

    }
}
