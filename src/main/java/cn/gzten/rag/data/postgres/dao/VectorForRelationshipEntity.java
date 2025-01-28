package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.storage.pojo.RagRelation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
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
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "LIGHTRAG_VDB_RELATION")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class VectorForRelationshipEntity extends RagRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surrogateId;
    private String workspace;

    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] contentVector;

    @CreationTimestamp
    private Timestamp createTime;
    @UpdateTimestamp
    private Timestamp updateTime;
}
