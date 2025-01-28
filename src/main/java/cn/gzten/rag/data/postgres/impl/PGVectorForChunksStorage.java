package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.DocChunkRepository;
import cn.gzten.rag.data.storage.BaseVectorStorage;
import cn.gzten.rag.data.storage.pojo.RagVectorChunk;
import cn.gzten.rag.llm.EmbeddingFunc;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static cn.gzten.rag.util.LightRagUtils.vectorToString;

@Slf4j
@Service("vectorForChunksStorage")
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGVectorForChunksStorage implements BaseVectorStorage<RagVectorChunk, RagVectorChunk> {
    private final DocChunkRepository docChunkRepo;
    private final EmbeddingFunc embeddingFunc;

    @Value("${rag.storage.cosine-better-than-threshold:0.2f}")
    private float cosineBetterThanThreshold;
    @Value("${rag.storage.workspace}")
    private String workspace;

    @Getter
    @Setter
    private Set<String> metaFields;

    @Override
    public Mono<Void> upsert(RagVectorChunk data) {
        log.info("upsert data: {}", data);
        String content = data.getContent();
        if (StringUtils.isBlank(content)) return Mono.empty();

        var contentVector = this.embeddingFunc.convert(content);

        return docChunkRepo.upsert(this.workspace, data.getId(),
                data.getTokens(),
                data.getChunkOrderIndex(),
                data.getFullDocId(),
                content,
                vectorToString(contentVector));

    }

    @Override
    public Flux<RagVectorChunk> query(String query, int topK) {
        var embedding = this.embeddingFunc.convert(query);
        return docChunkRepo.query(workspace, cosineBetterThanThreshold, vectorToString(embedding), topK).map(o -> {
            var res = new RagVectorChunk();
            res.setId(o.getId());
            res.setTokens(o.getTokens());
            res.setChunkOrderIndex(o.getChunkOrderIndex());
            res.setFullDocId(o.getFullDocId());
            res.setContent(o.getContent());
            return res;
        });
    }

    @Override
    public void indexDoneCallback() {

    }
}
