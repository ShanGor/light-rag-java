package cn.gzten.rag.data.postgres.dao;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.sql.Timestamp;

/**
 * CREATE TABLE LIGHTRAG_VDB_ENTITY (
 *  workspace VARCHAR(255),
 *  id VARCHAR(255),
 *  entity_name VARCHAR(255),
 *  content TEXT,
 *  content_vector VECTOR,
 *  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *  update_time TIMESTAMP,
 *  CONSTRAINT LIGHTRAG_VDB_ENTITY_PK PRIMARY KEY (workspace, id)
 * )
 */
@Data
@Entity
@Table(name = "LIGHTRAG_VDB_ENTITY")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class VectorForEntityEntity {
    @EmbeddedId
    private WorkspaceId cId;
    @Column(columnDefinition = "varchar(255)")
    private String entityName;
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
