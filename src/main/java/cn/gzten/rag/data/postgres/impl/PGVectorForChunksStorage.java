package cn.gzten.rag.data.postgres.impl;

import cn.gzten.rag.data.postgres.dao.DocChunkRepository;
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
@Service("vectorForChunksStorage")
@RequiredArgsConstructor
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGVectorForChunksStorage implements BaseVectorStorage {
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
    public void upsert(Map<String, Map<String, Object>> data) {
        log.info("upsert data: {}", data);
        for (var entry : data.entrySet()) {
            var id = entry.getKey();
            var item = entry.getValue();
            String content = (String) item.get("content");
            if (StringUtils.isBlank(content)) continue;

            var contentVector = this.embeddingFunc.convert(content);

            docChunkRepo.upsert(this.workspace, id,
                    (Integer) item.get("tokens"),
                    (Integer) item.get("chunk_order_index"),
                    (String) item.get("full_doc_id"),
                    content,
                    new PGvector(contentVector));
        }

    }

    @Override
    public List<Map<String, Object>> query(String query, int topK) {
        var result = new LinkedList<Map<String, Object>>();
        var res = docChunkRepo.query(workspace, cosineBetterThanThreshold, topK);
        var resMap = new HashMap<String, Object>();
        for (var o : res) {
            resMap.put("id", o.getCId().getId());
            resMap.put("content", o.getContent());
            resMap.put("tokens", o.getTokens());
            resMap.put("chunk_order_index", o.getChunkOrderIndex());
            resMap.put("full_doc_id", o.getFullDocId());
            result.add(resMap);
        };
        return result;
    }

    @Override
    public void indexDoneCallback() {

    }
}
