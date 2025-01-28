package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface DocStatusRepository extends CrudRepository<DocStatusEntity, Long> {
    @Query("SELECT e.id FROM DocStatusEntity e WHERE e.workspace = :ws and e.id in :ids")
    List<String> findByWorkspaceAndIds(@Param("ws") String workspace, @Param("ids")List<String> ids);

    @Query(value = """
        SELECT status as "status", COUNT(1) as "count"
         FROM LIGHTRAG_DOC_STATUS
         where workspace=:ws GROUP BY STATUS""", nativeQuery = true)
    List<DocStatusCount> getDocStatusCounts(@Param("ws") String workspace);

    @Query(value = "SELECT e FROM DocStatusEntity e where e.workspace=:ws and e.status=:status")
    List<DocStatusEntity> findByWorkspaceAndStatus(@Param("ws") String workspace, @Param("status") String status);

    @Modifying
    @Query(value = """
        insert into LIGHTRAG_DOC_STATUS(workspace,id,content_summary,content_length,chunks_count,status)
         values(:ws,:id,:cs,:cs,:cl,:st)
         on conflict(id,workspace) do update set
         content_summary = EXCLUDED.content_summary,
         content_length = EXCLUDED.content_length,
         chunks_count = EXCLUDED.chunks_count,
         status = EXCLUDED.status,
         updated_at = CURRENT_TIMESTAMP""", nativeQuery = true)
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("cs") String contentSummary,
                @Param("cl") Integer contentLength,
                @Param("cc") Integer chunksCount,
                @Param("st") String status);
}
