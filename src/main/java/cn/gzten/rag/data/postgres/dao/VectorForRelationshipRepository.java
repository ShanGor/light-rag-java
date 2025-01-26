package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface VectorForRelationshipRepository extends CrudRepository<VectorForRelationshipEntity, WorkspaceId> {

    @Modifying
    @Query(value = """
        INSERT INTO LIGHTRAG_VDB_RELATION (workspace, id, source_id,
         target_id, content, content_vector)
         VALUES (:ws, :id, :sid, :tid, :ct, :cv\\:\\:vector)
         ON CONFLICT (workspace,id) DO UPDATE
         SET source_id=EXCLUDED.source_id,
         target_id=EXCLUDED.target_id,
         content=EXCLUDED.content,
         content_vector=EXCLUDED.content_vector,
         update_time = CURRENT_TIMESTAMP""", nativeQuery = true)
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("sid") String sourceId,
                @Param("tid") String targetId,
                @Param("ct") String content,
                @Param("cv") String contentVector);


    @Query(value = """
        SELECT source_id, target_id FROM
         (SELECT id, source_id,target_id, 1 - (content_vector <=> :embedding\\:\\:vector) as distance
         FROM LIGHTRAG_VDB_RELATION where workspace=:ws)
         WHERE distance>:distance ORDER BY distance DESC  LIMIT :tk""", nativeQuery = true)
    List<VectorForRelationshipQueryResult> query(@Param("ws") String workspace,
                                                 @Param("distance") float distance,
                                                 @Param("embedding") String embedding,
                                                 @Param("tk") int topK);
}
