package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.LlmCache;

import java.util.Optional;

public interface LlmCacheStorage extends BaseKVStorage<LlmCache>{

    /**
     * For llm cache only.
     * @param mode
     * @param id
     * @return
     */
    Optional<LlmCache> getByModeAndId(String mode, String id);

}
