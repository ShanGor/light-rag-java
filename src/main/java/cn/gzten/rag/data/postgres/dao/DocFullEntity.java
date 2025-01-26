package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.pojo.FullDoc;
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
 * CREATE TABLE LIGHTRAG_DOC_FULL (
 *     id VARCHAR(255),
 *     workspace VARCHAR(255),
 *     doc_name VARCHAR(1024),
 *     content TEXT,
 *     meta JSONB,
 *     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     update_time TIMESTAMP,
 *     CONSTRAINT LIGHTRAG_DOC_FULL_PK PRIMARY KEY (workspace, id)
 * )
 */
@Data
@Entity
@Table(name = "LIGHTRAG_DOC_FULL")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class DocFullEntity implements FullDoc {
    @EmbeddedId
    private WorkspaceId cId;
    @Column(columnDefinition = "varchar(1024)")
    private String docName;
    @Column(columnDefinition = "text")
    private String content;
    @Column(columnDefinition = "jsonb")
    private String meta;
    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createTime;
    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT NULL")
    private Timestamp updateTime;
}
