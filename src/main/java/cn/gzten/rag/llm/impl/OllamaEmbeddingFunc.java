package cn.gzten.rag.llm.impl;

import cn.gzten.rag.llm.EmbeddingFunc;
import cn.gzten.rag.service.HttpService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;

@Service
@Slf4j
@Data
@ConditionalOnProperty(value = "rag.llm.embedding.provider", havingValue = "ollama")
public class OllamaEmbeddingFunc implements EmbeddingFunc {
    @Value("${rag.llm.embedding.model}")
    private String model;
    @Value("${rag.llm.embedding.url:http://localhost:11434/api/embed}")
    private URI url;
    @Resource
    private HttpService httpService;
    private Map<String, String> headers = Map.of("Content-Type", "application/json");

    @Value("${rag.llm.embedding.dimension}")
    private int dimension;
    @Value("${rag.llm.embedding.max-token-size}")
    private int maxTokenSize;
    @Value("${rag.llm.embedding.concurrent-limit:16}")
    private int concurrentLimit;

    @Override
    public float[] convert(String input) {
        Map<String, Object> body = Map.of("model", model, "input", input);
        log.info("ollama embedding request: {}", body);
        var resp = httpService.post(url, headers, body, OllamaEmbeddingResult.class);
        log.info("ollama embedding response: {}", resp);
        return resp.getEmbeddings()[0];
    }

    /**
     * {
     *   "model": "all-minilm",
     *   "embeddings": [[
     *     0.010071029, -0.0017594862, 0.05007221, 0.04692972, 0.054916814,
     *     0.008599704, 0.105441414, -0.025878139, 0.12958129, 0.031952348
     *   ]],
     *   "total_duration": 14143917,
     *   "load_duration": 1019500,
     *   "prompt_eval_count": 8
     * }
     */
    @Data
    public static class OllamaEmbeddingResult {
        private String model;
        private float[][] embeddings;
        private long total_duration;
        private long load_duration;
        private long prompt_eval_count;
    }
}
