package cn.gzten.rag.data.postgres.dao;

import jakarta.transaction.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.stream.Stream;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface VectorForRelationshipRepository extends CrudRepository<VectorForRelationshipEntity, Long> {

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
        SELECT * FROM
         (SELECT *, 1 - (content_vector <=> :embedding\\:\\:vector) as distance
         FROM LIGHTRAG_VDB_RELATION where workspace=:ws)
         WHERE distance>:distance ORDER BY distance DESC  LIMIT :tk""", nativeQuery = true)
    List<VectorForRelationshipEntity> query(@Param("ws") String workspace,
                                                 @Param("distance") float distance,
                                                 @Param("embedding") String embedding,
                                                 @Param("tk") int topK);


    @Query(value = "SELECT e FROM VectorForRelationshipEntity e")
    @Transactional
    Stream<VectorForRelationshipEntity> streamAll();

    @Transactional
    @Query(value = "SELECT e FROM VectorForRelationshipEntity e where e.graphProperties is null or e.graphProperties = ''")
    Stream<VectorForRelationshipEntity> streamAllForGraph();
}
