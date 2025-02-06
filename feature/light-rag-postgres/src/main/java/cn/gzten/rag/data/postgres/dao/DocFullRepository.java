package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface DocFullRepository extends CrudRepository<DocFullEntity, Long> {
    @Query("SELECT e.id FROM DocFullEntity e WHERE e.workspace = :ws and e.id in :ids")
    List<String> findByWorkspaceAndIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    @Query("SELECT e FROM DocFullEntity e WHERE e.workspace = :ws and e.id in :ids")
    List<DocFullEntity> findAllByWorkspaceAndIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    Optional<DocFullEntity> findByWorkspaceAndId(String workspace, String id);

    @Modifying
    @Query(value = """
        INSERT INTO LIGHTRAG_DOC_FULL (id, content, workspace)
         VALUES (:workspace, :id, :content)
         ON CONFLICT (workspace,id) DO UPDATE
         SET content = EXCLUDED.content,
         update_time = CURRENT_TIMESTAMP""", nativeQuery = true)
    void upsert(@Param("workspace") String workspace, @Param("id") String id, @Param("content") String content);
}
