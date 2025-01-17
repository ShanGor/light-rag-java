package cn.gzten.rag.data.postgres.dao;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.sql.Timestamp;

/**
 * CREATE TABLE LIGHTRAG_DOC_STATUS (
 *  workspace varchar(255) NOT NULL,
 *  id varchar(255) NOT NULL,
 *  content_summary varchar(255) NULL,
 *  content_length int4 NULL,
 *  chunks_count int4 NULL,
 *  status varchar(64) NULL,
 *  created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
 *  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
 *  CONSTRAINT LIGHTRAG_DOC_STATUS_PK PRIMARY KEY (workspace, id)
 * )
 */

@Data
@Entity
@Table(name = "LIGHTRAG_DOC_STATUS")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class DocStatusEntity {
    @EmbeddedId
    private WorkspaceId cId;
    @Column(columnDefinition = "varchar(255)")
    private String contentSummary;
    @Column(columnDefinition = "int4")
    private Integer contentLength;
    @Column(columnDefinition = "int4")
    private Integer chunksCount;
    @Column(columnDefinition = "varchar(64)")
    private String status;

    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;
    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT NULL")
    private Timestamp updatedAt;
}
