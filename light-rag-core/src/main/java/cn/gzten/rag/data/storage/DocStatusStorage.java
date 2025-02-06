package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.DocStatusStore;

import java.util.Map;

public interface DocStatusStorage <T extends DocStatusStore> extends BaseKVStorage <T> {
    Map<String, Integer> getStatusCounts();

    Map<String, DocProcessingStatus> getFailedDocs();

    Map<String, DocProcessingStatus> getPendingDocs();
}
