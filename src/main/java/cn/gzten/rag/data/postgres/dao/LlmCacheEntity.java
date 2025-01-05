package cn.gzten.rag.data.postgres.dao;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.sql.Timestamp;

/**
 * CREATE TABLE LIGHTRAG_LLM_CACHE (
 * 	  workspace varchar(255) NOT NULL,
 * 	  mode varchar(32) NOT NULL,
 * 	  id varchar(255) NOT NULL,
 *    original_prompt TEXT,
 *    returnValue TEXT,
 *    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *    update_time TIMESTAMP,
 * 	  CONSTRAINT LIGHTRAG_LLM_CACHE_PK PRIMARY KEY (workspace, mode, id)
 * );
 */
@Data
@Entity
@Table(name = "LIGHTRAG_LLM_CACHE")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class LlmCacheEntity {
    @EmbeddedId
    private WorkspaceId cId;
    @Column(columnDefinition = "TEXT")
    private String originalPrompt;
    @Column(columnDefinition = "TEXT")
    private String returnValue;

    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createTime;
    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT NULL")
    private Timestamp updateTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Id {
        @Column(columnDefinition = "varchar(255)")
        private String workspace;
        @Column(columnDefinition = "varchar(32)")
        private String mode;
        @Column(columnDefinition = "varchar(255)")
        private String id;
    }
}
