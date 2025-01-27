package cn.gzten.rag.data.storage;


import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface BaseVectorStorage<C, R> extends BaseStorage {
    Set<String> getMetaFields();
    void setMetaFields(Set<String> metaFields);

    void upsert(C data);

    <T> void traverse(Consumer<T> consumer);

    <T> void cache(T data);

    List<R> query(String query, int topK);
}
