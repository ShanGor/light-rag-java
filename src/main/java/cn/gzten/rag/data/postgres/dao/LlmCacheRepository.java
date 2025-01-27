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
public interface LlmCacheRepository extends ReactiveCrudRepository<LlmCacheEntity, String> {
    @Query("SELECT e.id FROM lightrag_llm_cache e WHERE e.workspace = :ws and e.id in :ids")
    Flux<String> findByWorkspaceAndIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    Mono<LlmCacheEntity> findByWorkspaceAndModeAndId(String workspace, String mode, String id);

    @Modifying
    @Query(value = """
        INSERT INTO lightrag_llm_cache(workspace,id,original_prompt,return_value,mode)
         VALUES (:ws, :id, :op, :rv, :mode)
         ON CONFLICT (workspace,mode,id) DO UPDATE
         SET original_prompt = EXCLUDED.original_prompt,
         return_value=EXCLUDED.return_value,
         mode=EXCLUDED.mode,
         update_time = CURRENT_TIMESTAMP""")
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("op") String originalPrompt,
                @Param("rv") String returnValue,
                @Param("mode") String mode);
}
