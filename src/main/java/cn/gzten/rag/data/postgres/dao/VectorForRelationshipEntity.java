package cn.gzten.rag.data.postgres.dao;

import io.r2dbc.postgresql.codec.Vector;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@Table("lightrag_vdb_relation")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class VectorForRelationshipEntity {
    @Id
    private String surrogateId;
    private String workspace;
    private String id;

    private String sourceId;

    private String targetId;

    private String content;

    private Vector contentVector;

    private String graphProperties;

    private Timestamp createTime;

    private Timestamp updateTime;
}
