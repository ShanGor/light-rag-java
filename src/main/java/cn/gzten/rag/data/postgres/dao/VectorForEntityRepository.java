package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface VectorForEntityRepository extends ReactiveCrudRepository<VectorForEntityEntity, String> {

    @Modifying
    @Query(value = """
        INSERT INTO lightrag_vdb_entity (workspace, id, entity_name, content, content_vector)
         VALUES (:ws, :id, :enm, :ct, :cv\\:\\:vector)
         ON CONFLICT (workspace,id) DO UPDATE
         SET entity_name=EXCLUDED.entity_name,
         content=EXCLUDED.content,
         content_vector=EXCLUDED.content_vector,
         update_time=CURRENT_TIMESTAMP""")
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("enm") String entityName,
                @Param("ct") String content,
                @Param("cv") String contentVector);


    @Query(value = """
        SELECT entity_name FROM
         (SELECT id, entity_name, 1 - (content_vector <=> :embedding\\:\\:vector) as distance
         FROM LIGHTRAG_VDB_ENTITY where workspace=:ws)
         WHERE distance>:distance ORDER BY distance DESC  LIMIT :tk""")
    Flux<String> query(@Param("ws") String workspace,
                       @Param("distance") float distance,
                       @Param("embedding") String embedding,
                       @Param("tk") int topK);
}
