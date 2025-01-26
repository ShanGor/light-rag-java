package cn.gzten.rag.data.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BaseKVStorage <T> extends BaseStorage{

    List<String> allKeys();
    Optional<T> getById(String id);

    List<T> getByIds(List<String> ids);
    Set<String> filterKeys(List<String> data);
    void upsert(T data);
    void drop();

}
