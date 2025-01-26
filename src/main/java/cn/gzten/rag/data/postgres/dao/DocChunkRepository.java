package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface DocChunkRepository extends CrudRepository<DocChunkEntity, WorkspaceId> {
    @Query("SELECT e.cId.id FROM DocChunkEntity e WHERE e.cId.workspace = :ws and e.cId.id in :ids")
    List<String> findByWorkspaceAndIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    @Modifying
    @Query(value = """
        INSERT INTO LIGHTRAG_DOC_CHUNKS (workspace, id, tokens,
         chunk_order_index, full_doc_id, content, content_vector)
         VALUES (:ws, :id, :tk, :coi, :fdi, :ct, :cv\\:\\:vector)
         ON CONFLICT (workspace,id) DO UPDATE
         SET tokens=EXCLUDED.tokens,
         chunk_order_index=EXCLUDED.chunk_order_index,
         full_doc_id=EXCLUDED.full_doc_id,
         content = EXCLUDED.content,
         content_vector=EXCLUDED.content_vector,
         update_time = CURRENT_TIMESTAMP""", nativeQuery = true)
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
         FROM LIGHTRAG_DOC_CHUNKS where workspace=:ws)
         WHERE distance>:distance ORDER BY distance DESC  LIMIT :tk""", nativeQuery = true)
    List<DocChunkEntity> query(@Param("ws") String workspace,
                               @Param("distance") float distance,
                               @Param("embedding") String embedding,
                               @Param("tk") int topK);
}
