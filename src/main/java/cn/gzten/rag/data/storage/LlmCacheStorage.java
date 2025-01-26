package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.LlmCache;

import java.util.Optional;

public interface LlmCacheStorage<T extends LlmCache> extends BaseKVStorage<T>{

    /**
     * For llm cache only.
     * @param mode
     * @param id
     * @return
     */
    Optional<T> getByModeAndId(String mode, String id);

}
