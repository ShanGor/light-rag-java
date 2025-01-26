package cn.gzten.rag.data.storage;

import cn.gzten.rag.data.pojo.DocStatusStore;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Map;

@Data
@Builder
public class DocProcessingStatus implements DocStatusStore {
    private String workspace;
    private String id;
    private String contentSummary;
    private int contentLength;
    private DocStatus status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int chunksCount;
    private String error;
    private Map<String, Object> metadata;
}
