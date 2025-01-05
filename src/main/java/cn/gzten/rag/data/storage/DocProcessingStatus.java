package cn.gzten.rag.data.storage;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

@Data
@Builder
public class DocProcessingStatus {

    private String contentSummary;
    private int contentLength;
    private DocStatus status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Optional<Integer> chunksCount;
    private Optional<String> error;
    private Map<String, Object> metadata;
}
