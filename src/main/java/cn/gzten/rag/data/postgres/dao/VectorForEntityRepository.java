package cn.gzten.rag.data.postgres.dao;

import com.pgvector.PGvector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface VectorForEntityRepository extends CrudRepository<VectorForEntityEntity, WorkspaceId> {

    @Modifying
    @Query(value = """
        INSERT INTO LIGHTRAG_VDB_ENTITY (workspace, id, entity_name, content, content_vector)
         VALUES (:ws, :id, :enm, :ct, :cv)
         ON CONFLICT (workspace,id) DO UPDATE
         SET entity_name=EXCLUDED.entity_name,
         content=EXCLUDED.content,
         content_vector=EXCLUDED.content_vector,
         update_time=CURRENT_TIMESTAMP""", nativeQuery = true)
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("enm") String entityName,
                @Param("ct") String content,
                @Param("cv") PGvector contentVector);


    @Query(value = """
        SELECT entity_name FROM
         (SELECT id, entity_name, 1 - (content_vector <=> '[{embedding_string}]'\\:\\:vector) as distance
         FROM LIGHTRAG_VDB_ENTITY where workspace=:ws)
         WHERE distance>:distance ORDER BY distance DESC  LIMIT :tk""", nativeQuery = true)
    List<String> query(@Param("ws") String workspace,
                       @Param("distance") float distance,
                       @Param("tk") int topK);
}
