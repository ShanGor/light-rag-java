package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.LlmCache;
import reactor.core.publisher.Mono;

public interface LlmCacheStorage extends BaseKVStorage<LlmCache>{

    /**
     * For llm cache only.
     * @param mode
     * @param id
     * @return
     */
    Mono<LlmCache> getByModeAndId(String mode, String id);

}
