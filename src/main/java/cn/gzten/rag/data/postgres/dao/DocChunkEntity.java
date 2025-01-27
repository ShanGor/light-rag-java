package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.pojo.TextChunk;
import io.r2dbc.postgresql.codec.Vector;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@Table("lightrag_doc_chunks")
public class DocChunkEntity implements TextChunk {
    @Id
    private String surrogateId;
    private String workspace;
    private String id;
    private String fullDocId;
    private Integer chunkOrderIndex;
    private Integer tokens;
    private String content;
    @Getter(AccessLevel.NONE)
    private Vector contentVector;
    private Timestamp createTime;
    private Timestamp updateTime;

    @Override
    public float[] getContentVector() {
        return contentVector.getVector();
    }
}
