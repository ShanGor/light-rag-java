package cn.gzten.rag.llm.impl;

import cn.gzten.rag.llm.EmbeddingFunc;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Data
@ConditionalOnProperty(value = "rag.llm.embedding.provider", havingValue = "ollama")
public class OllamaEmbeddingFunc implements EmbeddingFunc {
    @Value("${rag.llm.embedding.model}")
    private String model;
    @Value("${rag.llm.embedding.url:http://localhost:11434/api/embeddings}")
    private String url;

    @Value("${rag.llm.embedding.dimension}")
    private int dimension;
    @Value("${rag.llm.embedding.max-token-size}")
    private int maxTokenSize;
    @Value("${rag.llm.embedding.concurrent-limit:16}")
    private int concurrentLimit;


    @Override
    public float[] convert(String input) {
        return new float[0];
    }
}
