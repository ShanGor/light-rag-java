package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.VectorForEntityRepository;
import cn.gzten.rag.data.storage.BaseVectorStorage;
import cn.gzten.rag.llm.EmbeddingFunc;
import com.pgvector.PGvector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGVectorForEntitiesStorage implements BaseVectorStorage {
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
    public void upsert(Map<String, Map<String, Object>> data) {
        log.info("upsert data: {}", data);
        for (var entry : data.entrySet()) {
            var id = entry.getKey();
            var item = entry.getValue();
            String content = (String) item.get("content");
            if (StringUtils.isBlank(content)) continue;

            var contentVector = this.embeddingFunc.convert(content);

            vectorForEntityRepo.upsert(this.workspace, id,
                    (String) item.get("entity_name"),
                    content,
                    new PGvector(contentVector));
        }

    }

    @Override
    public List<Map<String, Object>> query(String query, int topK) {
        var result = new LinkedList<Map<String, Object>>();
        var entityNames = vectorForEntityRepo.query(this.workspace,
                cosineBetterThanThreshold,
                topK);
        var resMap = new HashMap<String, Object>();
        for (var entityName : entityNames) {
            resMap.put("entity_name", entityName);
            result.add(resMap);
        }

        return result;
    }

    @Override
    public void indexDoneCallback() {

    }
}
