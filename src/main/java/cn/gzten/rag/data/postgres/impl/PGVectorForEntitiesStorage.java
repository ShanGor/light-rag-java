package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.VectorForEntityRepository;
import cn.gzten.rag.data.storage.BaseVectorStorage;
import cn.gzten.rag.data.storage.pojo.RagEntity;
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

import java.util.*;

import static cn.gzten.rag.util.LightRagUtils.vectorToString;

@Slf4j
@Service("entityStorage")
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGVectorForEntitiesStorage implements BaseVectorStorage<RagEntity, String> {
    private final VectorForEntityRepository vectorForEntityRepo;
    private final EmbeddingFunc embeddingFunc;

    @Value("${rag.storage.cosine-better-than-threshold:0.2f}")
    private float cosineBetterThanThreshold;
    @Value("${rag.storage.workspace}")
    private String workspace;

    @Getter
    @Setter
    private Set<String> metaFields;

    @Override
    public void upsert(RagEntity data) {
        log.info("upsert data: {}", data);
        String content = data.getContent();
        if (StringUtils.isBlank(content)) return;

        var contentVector = this.embeddingFunc.convert(content);

        vectorForEntityRepo.upsert(this.workspace, data.getId(), data.getEntityName(),
                content, vectorToString(contentVector));

    }

    @Override
    public Flux<String> query(String query, int topK) {
        var embeddingString = vectorToString(embeddingFunc.convert(query));
        return vectorForEntityRepo.query(this.workspace,
                cosineBetterThanThreshold,
                embeddingString,
                topK);
    }

    @Override
    public void indexDoneCallback() {

    }
}
