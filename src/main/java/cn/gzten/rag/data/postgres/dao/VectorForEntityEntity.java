package cn.gzten.rag.data.postgres.dao;

import io.r2dbc.postgresql.codec.Vector;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@Table("lightrag_vdb_entity")
public class VectorForEntityEntity {
    @Id
    private String surrogateId;
    private String workspace;
    private String id;

    private String entityName;

    private String content;

    private Vector contentVector;

    private String graphProperties;

    private Timestamp createTime;

    private Timestamp updateTime;
}
