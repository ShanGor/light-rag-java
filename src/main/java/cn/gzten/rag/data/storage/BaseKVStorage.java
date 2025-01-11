package cn.gzten.rag.data.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BaseKVStorage <T> extends BaseStorage{

    List<String> allKeys();
    Optional<T> getById(String id);

    /**
     * For llm cache only.
     * @param mode
     * @param id
     * @return
     */
    Optional<T> getByModeAndId(String mode, String id);
    List<T> getByIds(List<String> ids);
    Set<String> filterKeys(List<String> data);
    void upsert(Map<String, Object> data);
    void drop();

}
