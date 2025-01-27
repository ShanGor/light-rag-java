package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.pojo.NullablePair;
import cn.gzten.rag.data.postgres.dao.VectorForRelationshipRepository;
import cn.gzten.rag.data.storage.BaseVectorStorage;
import cn.gzten.rag.data.storage.pojo.RagRelation;
import cn.gzten.rag.llm.EmbeddingFunc;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

import static cn.gzten.rag.util.LightRagUtils.vectorToString;

@Slf4j
@Service("relationshipStorage")
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGVectorForRelationshipStorage implements BaseVectorStorage<RagRelation, NullablePair<String, String>> {
    private final VectorForRelationshipRepository vectorForRelationRepo;
    private final EmbeddingFunc embeddingFunc;

    @Value("${rag.storage.cosine-better-than-threshold:0.2f}")
    private float cosineBetterThanThreshold;
    @Value("${rag.storage.workspace}")
    private String workspace;

    @Getter
    @Setter
    private Set<String> metaFields;

    @Override
    public void upsert(RagRelation data) {
        log.info("upsert data: {}", data);
        String content = data.getContent();
        if (StringUtils.isBlank(content)) return;

        var contentVector = this.embeddingFunc.convert(content);

        vectorForRelationRepo.upsert(this.workspace,
                data.getId(),
                data.getSourceId(),
                data.getTargetId(),
                content,
                vectorToString(contentVector));

    }

    @Override
    public <T> void traverse(Consumer<T> consumer) {
        vectorForRelationRepo.streamAll().forEach(o -> consumer.accept((T) o));
    }

    @Override
    public <T> void cache(T data) {

    }

    @Override
    public List<NullablePair<String, String>> query(String query, int topK) {
        var result = new LinkedList<NullablePair<String, String>>();
        var embedding = this.embeddingFunc.convert(query);
        var res = vectorForRelationRepo.query(this.workspace, cosineBetterThanThreshold, vectorToString(embedding), topK);
        for (var o : res) {
            result.add(new NullablePair<>(o.getSourceId(), o.getTargetId()));
        }
        return result;
    }

    @Override
    public void indexDoneCallback() {
        log.info("Index done for PGVectorForRelationshipStorage: {}", workspace);
    }
}
