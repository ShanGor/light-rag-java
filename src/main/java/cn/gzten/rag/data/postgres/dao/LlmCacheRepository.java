package cn.gzten.rag.data.postgres.dao;

import jakarta.transaction.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface LlmCacheRepository extends CrudRepository<LlmCacheEntity, Long> {
    @Query("SELECT e.id FROM LlmCacheEntity e WHERE e.workspace = :ws and e.id in :ids")
    List<String> findByWorkspaceAndIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    Optional<LlmCacheEntity> findByWorkspaceAndId(String workspace, String id);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO LIGHTRAG_LLM_CACHE(workspace,id,original_prompt,return_value,mode)
         VALUES (:ws, :id, :op, :rv, :mode)
         ON CONFLICT (workspace,mode,id) DO UPDATE
         SET original_prompt = EXCLUDED.original_prompt,
         return_value=EXCLUDED.return_value,
         mode=EXCLUDED.mode,
         update_time = CURRENT_TIMESTAMP""", nativeQuery = true)
    void upsert(@Param("ws") String workspace,
                @Param("id") String id,
                @Param("op") String originalPrompt,
                @Param("rv") String returnValue,
                @Param("mode") String mode);
}
