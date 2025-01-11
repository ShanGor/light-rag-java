package cn.gzten.rag.data.storage;

import java.util.Map;

public interface DocStatusStorage <T> extends BaseKVStorage <T> {
    Map<String, Integer> getStatusCounts();

    Map<String, DocProcessingStatus> getFailedDocs();

    Map<String, DocProcessingStatus> getPendingDocs();
}
