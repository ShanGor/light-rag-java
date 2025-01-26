package cn.gzten.rag.data.storage;


import java.util.List;
import java.util.Set;

public interface BaseVectorStorage<C, R> extends BaseStorage {
    Set<String> getMetaFields();
    void setMetaFields(Set<String> metaFields);

    void upsert(C data);

    List<R> query(String query, int topK);
}
