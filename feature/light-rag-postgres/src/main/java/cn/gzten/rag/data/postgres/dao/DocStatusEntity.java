package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.pojo.DocStatusStore;
import cn.gzten.rag.data.storage.DocStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
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
public class DocStatusEntity implements DocStatusStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surrogateId;
    private String workspace;
    private String id;

    private String contentSummary;

    private int contentLength;

    private int chunksCount;

    @Getter(AccessLevel.NONE)
    private String status;

    @CreationTimestamp
    private Timestamp createdAt;
    @UpdateTimestamp

    private Timestamp updatedAt;

    @Override
    public DocStatus getStatus() {
        return DocStatus.valueOf(status);
    }
}
