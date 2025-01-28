package cn.gzten.rag.data.postgres.dao;

import cn.gzten.rag.data.pojo.LlmCache;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
@Table(name = "LIGHTRAG_LLM_CACHE")
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class LlmCacheEntity extends LlmCache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surrogateId;
    private String workspace;
    private String id;
    private String mode;

    private String originalPrompt;

    private String returnValue;

    @CreationTimestamp
    private Timestamp createTime;
    @UpdateTimestamp
    private Timestamp updateTime;
}
