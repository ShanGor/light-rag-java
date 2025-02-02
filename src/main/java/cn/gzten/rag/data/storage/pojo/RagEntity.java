package cn.gzten.rag.data.storage.pojo;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public class RagEntity {
    protected String id;
    protected String content;
    protected String entityName;
    protected String graphProperties;
    protected Integer graphNodeDegree;
}
