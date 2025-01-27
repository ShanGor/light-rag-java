package cn.gzten.rag.data.postgres.dao;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.sql.Timestamp;

/**
 * CREATE TABLE LIGHTRAG_VDB_RELATION (
 *      id VARCHAR(255),
 *      workspace VARCHAR(255),
 *      source_id VARCHAR(256),
 *      target_id VARCHAR(256),
 *      content TEXT,
 *      content_vector VECTOR,
 *      create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *      update_time TIMESTAMP,
 * 	    CONSTRAINT LIGHTRAG_VDB_RELATION_PK PRIMARY KEY (workspace, id)
 * )
 */
@Data
@Entity
@Table(name = "LIGHTRAG_VDB_RELATION")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class VectorForRelationshipEntity {
    @EmbeddedId
    private WorkspaceId cId;
    @Column(columnDefinition = "varchar(256)")
    private String sourceId;
    @Column(columnDefinition = "varchar(256)")
    private String targetId;
    @Column(columnDefinition = "text")
    private String content;
    @Column(columnDefinition = "VECTOR")
    @Convert(converter = PGVectorConverter.class)
    private float[] contentVector;
    @Column(columnDefinition = "text")
    private String graphProperties;
    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createTime;
    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT NULL")
    private Timestamp updateTime;
}
