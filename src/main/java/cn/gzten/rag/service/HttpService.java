package cn.gzten.rag.service;

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
}
