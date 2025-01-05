package cn.gzten.rag.data.postgres.dao;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.sql.Timestamp;

/**
 * CREATE TABLE LIGHTRAG_DOC_CHUNKS (
 *     workspace VARCHAR(255),
 *     id VARCHAR(255),
 *     full_doc_id VARCHAR(256),
 *     chunk_order_index INTEGER,
 *     tokens INTEGER,
 *     content TEXT,
 *     content_vector VECTOR,
 *     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     update_time TIMESTAMP,
 * 	   CONSTRAINT LIGHTRAG_DOC_CHUNKS_PK PRIMARY KEY (workspace, id)
 * )
 */
@Data
@Entity
@Table(name = "LIGHTRAG_DOC_CHUNKS")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class DocChunkEntity {
    @EmbeddedId
    private WorkspaceId cId;
    @Column(columnDefinition = "varchar(256)")
    private String fullDocId;
    @Column(columnDefinition = "INTEGER")
    private Integer chunkOrderIndex;
    @Column(columnDefinition = "INTEGER")
    private Integer tokens;
    @Column(columnDefinition = "text")
    private String content;
    @Column(columnDefinition = "VECTOR")
    @Convert(converter = PGVectorConverter.class)
    private float[] contentVector;

    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createTime;
    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT NULL")
    private Timestamp updateTime;
}
