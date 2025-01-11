package cn.gzten.rag.service;

import java.net.URI;
import java.util.Map;

public interface HttpService {
    <T> T post(URI url,
               Map<String, String> headers,
               Map<String, Object> requestBody,
               Class<T> clazz) ;
}
