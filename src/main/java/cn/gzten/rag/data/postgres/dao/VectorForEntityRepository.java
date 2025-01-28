package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.stream.Stream;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface VectorForEntityRepository extends CrudRepository<VectorForEntityEntity, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO LIGHTRAG_VDB_ENTITY (workspace, id, entity_name, content, content_vector)
         VALUES (:ws, :id, :enm, :ct, :cv\\:\\:vector)
         ON CONFLICT (workspace,id) DO UPDATE
         SET entity_name=EXCLUDED.entity_name,
         content=EXCLUDED.content,
         content_vector=EXCLUDED.content_vector,
         update_time=CURRENT_TIMESTAMP""", nativeQuery = true)
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("enm") String entityName,
                @Param("ct") String content,
                @Param("cv") String contentVector);


    @Query(value = """
        SELECT entity_name FROM
         (SELECT id, entity_name, 1 - (content_vector <=> :embedding\\:\\:vector) as distance
         FROM LIGHTRAG_VDB_ENTITY where workspace=:ws)
         WHERE distance>:distance ORDER BY distance DESC  LIMIT :tk""", nativeQuery = true)
    List<String> query(@Param("ws") String workspace,
                       @Param("distance") float distance,
                       @Param("embedding") String embedding,
                       @Param("tk") int topK);

    @Query(value = "SELECT e FROM VectorForEntityEntity e")
    Stream<VectorForEntityEntity> streamAll();
}
