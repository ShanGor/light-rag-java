package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.pojo.TextChunk;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
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
public class DocChunkEntity implements TextChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surrogateId;
    private String workspace;
    private String id;

    private String fullDocId;

    private Integer chunkOrderIndex;

    private Integer tokens;

    private String content;

    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] contentVector;

    @CreationTimestamp
    private Timestamp createTime;
    @UpdateTimestamp
    private Timestamp updateTime;
}
