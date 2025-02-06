package cn.gzten.rag.service;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

public interface HttpService {
    <T> T post(URI url,
               Map<String, String> headers,
               Map<String, Object> requestBody,
               Class<T> clazz) ;
    <T> Mono<T> postAsync(URI url,
                          Map<String, String> headers,
                          Object requestBody,
                          Class<T> clazz) ;

    Flux<ServerSentEvent<String>> postSeverSentEvent(URI url, Map<String, String> headers, Object requestBody, boolean raw);
}
