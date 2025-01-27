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
    public void upsert(RagVectorChunk data) {
        log.info("upsert data: {}", data);
        String content = data.getContent();
        if (StringUtils.isBlank(content)) return;

        var contentVector = this.embeddingFunc.convert(content);

        docChunkRepo.upsert(this.workspace, data.getId(),
                data.getTokens(),
                data.getChunkOrderIndex(),
                data.getFullDocId(),
                content,
                vectorToString(contentVector));

    }

    @Override
    public List<RagVectorChunk> query(String query, int topK) {
        var result = new LinkedList<RagVectorChunk>();
        var embedding = this.embeddingFunc.convert(query);
        var res = docChunkRepo.query(workspace, cosineBetterThanThreshold, vectorToString(embedding), topK);

        for (var o : res) {
            var resMap = new RagVectorChunk();
            resMap.setId(o.getCId().getId());
            resMap.setTokens(o.getTokens());
            resMap.setChunkOrderIndex(o.getChunkOrderIndex());
            resMap.setFullDocId(o.getFullDocId());
            resMap.setContent(o.getContent());
            result.add(resMap);
        };
        return result;
    }

    @Override
    public void indexDoneCallback() {

    }
}
