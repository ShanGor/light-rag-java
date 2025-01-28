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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surrogateId;
    private String workspace;
    private String id;

    private String entityName;

    private String content;

    @Convert(converter = PGVectorConverter.class)
    private float[] contentVector;

    private String graphProperties;

    @CreationTimestamp
    private Timestamp createTime;
    @UpdateTimestamp
    private Timestamp updateTime;
}
