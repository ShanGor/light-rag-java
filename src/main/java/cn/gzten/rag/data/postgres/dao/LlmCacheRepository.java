package cn.gzten.rag.data.postgres.dao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public interface LlmCacheRepository extends CrudRepository<LlmCacheEntity, LlmCacheEntity.Id> {
    @Query("SELECT e.cId.id FROM LlmCacheEntity e WHERE e.cId.workspace = :ws and e.cId.id in :ids")
    List<String> findByWorkspaceAndIds(@Param("ws")String workspace, @Param("ids")List<String> ids);

    @Modifying
    @Query(value = """
        INSERT INTO LIGHTRAG_LLM_CACHE(workspace,id,original_prompt,return_value,mode)
         VALUES (:ws, :id, :op, :rv, :mode)
         ON CONFLICT (workspace,id) DO UPDATE
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
