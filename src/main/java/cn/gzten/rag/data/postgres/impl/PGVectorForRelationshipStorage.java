package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.VectorForRelationshipRepository;
import cn.gzten.rag.data.storage.BaseVectorStorage;
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

import static cn.gzten.rag.util.LightRagUtils.vectorToString;

@Slf4j
@Service("relationshipStorage")
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGVectorForRelationshipStorage implements BaseVectorStorage {
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
    public void upsert(Map<String, Map<String, Object>> data) {
        log.info("upsert data: {}", data);
        for (var entry : data.entrySet()) {
            var id = entry.getKey();
            var item = entry.getValue();
            String content = (String) item.get("content");
            if (StringUtils.isBlank(content)) continue;

            var contentVector = this.embeddingFunc.convert(content);

            vectorForRelationRepo.upsert(this.workspace,
                    id,
                    (String) item.get("src_id"),
                    (String) item.get("tgt_id"),
                    content,
                    vectorToString(contentVector));
        }

    }

    @Override
    public List<Map<String, Object>> query(String query, int topK) {
        var result = new LinkedList<Map<String, Object>>();
        var embedding = this.embeddingFunc.convert(query);
        var res = vectorForRelationRepo.query(this.workspace, cosineBetterThanThreshold, vectorToString(embedding), topK);
        var resMap = new HashMap<String, Object>();
        for (var o : res) {
            resMap.put("src_id", o.getSourceId());
            resMap.put("tgt_id", o.getTargetId());
            result.add(resMap);
        }
        return result;
    }

    @Override
    public void indexDoneCallback() {
        log.info("Index done for PGVectorForRelationshipStorage: {}", workspace);
    }
}
