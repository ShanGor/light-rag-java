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
public interface DocFullRepository extends ReactiveCrudRepository<DocFullEntity, String> {
    @Query("SELECT e.id FROM lightrag_doc_full e WHERE e.workspace = :ws and e.id in :ids")
    Flux<String> findByWorkspaceAndIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    @Query("SELECT * FROM lightrag_doc_full e WHERE e.workspace = :ws and e.id in :ids")
    Flux<DocFullEntity> findByIds(@Param("ws")String workspace, @Param("ids")List<String> ids);
    Mono<DocFullEntity> findByWorkspaceAndId(String workspace, String id);

    @Modifying
    @Query(value = """
        INSERT INTO lightrag_doc_full (id, content, workspace)
         VALUES (:workspace, :id, :content)
         ON CONFLICT (workspace,id) DO UPDATE
         SET content = EXCLUDED.content,
         update_time = CURRENT_TIMESTAMP""")
    void upsert(@Param("workspace") String workspace, @Param("id") String id, @Param("content") String content);
}
