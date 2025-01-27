package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface DocChunkRepository extends ReactiveCrudRepository<DocChunkEntity, String> {
    @Query("SELECT id FROM lightrag_doc_chunks e WHERE e.workspace = :ws and e.id in :ids")
    Flux<String> findByWorkspaceAndIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    @Query("SELECT * FROM lightrag_doc_chunks e WHERE e.workspace = :ws and e.id in :ids")
    Flux<DocChunkEntity> findByIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    Mono<DocChunkEntity> findByWorkspaceAndId(String workspace, String id);

    @Modifying
    @Query(value = """
        INSERT INTO lightrag_doc_chunks (workspace, id, tokens,
         chunk_order_index, full_doc_id, content, content_vector)
         VALUES (:ws, :id, :tk, :coi, :fdi, :ct, :cv\\:\\:vector)
         ON CONFLICT (workspace,id) DO UPDATE
         SET tokens=EXCLUDED.tokens,
         chunk_order_index=EXCLUDED.chunk_order_index,
         full_doc_id=EXCLUDED.full_doc_id,
         content = EXCLUDED.content,
         content_vector=EXCLUDED.content_vector,
         update_time = CURRENT_TIMESTAMP""")
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("tk") Integer tokens,
                @Param("coi") Integer chunkOrderIndex,
                @Param("fdi") String fullDocId,
                @Param("ct") String content,
                @Param("cv") String contentVector);

    @Query(value = """
        SELECT * FROM
         (SELECT id, 1 - (content_vector <=> :embedding\\:\\:vector) as distance
         FROM lightrag_doc_chunks where workspace=:ws)
         WHERE distance>:distance ORDER BY distance DESC  LIMIT :tk""")
    Flux<DocChunkEntity> query(@Param("ws") String workspace,
                               @Param("distance") float distance,
                               @Param("embedding") String embedding,
                               @Param("tk") int topK);
}
