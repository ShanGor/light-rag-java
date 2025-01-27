package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.pojo.LlmCache;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Table("lightrag_llm_cache")
public class LlmCacheEntity extends LlmCache {
    @Id
    private String surrogateId;

    private String workspace;
    private String mode;
    private String id;

    private String originalPrompt;

    private String returnValue;


    private Timestamp createTime;

    private Timestamp updateTime;
}
