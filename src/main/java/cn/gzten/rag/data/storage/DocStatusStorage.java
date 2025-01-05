package cn.gzten.rag.data.storage;

import java.util.Map;

public interface DocStatusStorage extends BaseKVStorage {
    Map<String, Integer> getStatusCounts();

    Map<String, DocProcessingStatus> getFailedDocs();

    Map<String, DocProcessingStatus> getPendingDocs();
}
