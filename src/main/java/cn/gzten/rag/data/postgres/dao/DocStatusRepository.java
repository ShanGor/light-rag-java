package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface DocStatusRepository extends ReactiveCrudRepository<DocStatusEntity, String> {
    @Query("SELECT e.id FROM lightrag_doc_status e WHERE e.workspace = :ws and e.id in :ids")
    Flux<String> findByWorkspaceAndIds(@Param("ws") String workspace, @Param("ids")List<String> ids);

    @Query(value = """
        SELECT status as "status", COUNT(1) as "count"
         FROM lightrag_doc_status
         where workspace=:ws GROUP BY STATUS""")
    Flux<DocStatusCount> getDocStatusCounts(@Param("ws") String workspace);

    @Query(value = "SELECT e FROM lightrag_doc_status e where e.workspace=:ws and e.status=:status")
    Flux<DocStatusEntity> findByWorkspaceAndStatus(@Param("ws") String workspace, @Param("status") String status);

    @Modifying
    @Query(value = """
        insert into lightrag_doc_status(workspace,id,content_summary,content_length,chunks_count,status)
         values(:ws,:id,:cs,:cs,:cl,:st)
         on conflict(id,workspace) do update set
         content_summary = EXCLUDED.content_summary,
         content_length = EXCLUDED.content_length,
         chunks_count = EXCLUDED.chunks_count,
         status = EXCLUDED.status,
         updated_at = CURRENT_TIMESTAMP""")
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("cs") String contentSummary,
                @Param("cl") Integer contentLength,
                @Param("cc") Integer chunksCount,
                @Param("st") String status);
}
