package cn.gzten.rag.data.storage.pojo;

import lombok.Data;

@Data
public class RagRelation {
    private String id;
    private String content;
    private String sourceId;
    private String targetId;
}
