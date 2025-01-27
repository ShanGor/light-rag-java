package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.pojo.DocStatusStore;
import cn.gzten.rag.data.storage.DocStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@Table("lightrag_doc_status")
public class DocStatusEntity implements DocStatusStore {
    @Id
    private String surrogateId;
    private String workspace;
    private String id;
    private String contentSummary;
    private int contentLength;
    private int chunksCount;
    @Getter(AccessLevel.NONE)
    private String status;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    @Override
    public DocStatus getStatus() {
        return DocStatus.valueOf(status);
    }
}
