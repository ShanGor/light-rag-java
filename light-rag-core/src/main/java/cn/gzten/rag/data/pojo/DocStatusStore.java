package cn.gzten.rag.data.pojo;

import cn.gzten.rag.data.storage.DocStatus;

public interface DocStatusStore {
    String getId();
    String getWorkspace();
    String getContentSummary();
    int getContentLength();
    int getChunksCount();
    DocStatus getStatus();
}
