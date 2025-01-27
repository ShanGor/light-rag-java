package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.pojo.FullDoc;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@Table("lightrag_doc_full")
public class DocFullEntity implements FullDoc {
    @Id
    private String surrogateId;
    private String workspace;
    private String id;

    private String docName;

    private String content;

    private String meta;

    private Timestamp createTime;

    private Timestamp updateTime;
}
