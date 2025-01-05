package cn.gzten.rag.data.storage;


import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BaseVectorStorage extends BaseStorage{
    Set<String> getMetaFields();
    void setMetaFields(Set<String> metaFields);

    void upsert(Map<String, Map<String, Object>> data);

    List<Map<String, Object>> query(String query, int topK);
}
