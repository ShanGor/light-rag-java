package cn.gzten.rag.data.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BaseKVStorage extends BaseStorage{

    List<String> allKeys();
    Object getById(String id);

    /**
     * For llm cache only.
     * @param mode
     * @param id
     * @return
     */
    Object getByModeAndId(String mode, String id);
    List<Object> getByIds(List<String> ids);
    Set<String> filterKeys(List<String> data);
    void upsert(Map<String, Object> data);
    void drop();

}
