package cn.gzten.rag.data.storage.pojo;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public class RagRelation {
    protected String id;
    protected String content;
    protected String sourceId;
    protected String targetId;
    protected String graphProperties;
}
