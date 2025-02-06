package cn.gzten.rag.data.storage.pojo;

import lombok.Data;

@Data
public class RagVectorChunk {
    private String id;
    private String fullDocId;
    private Integer chunkOrderIndex;
    private Integer tokens;
    private String content;
}
