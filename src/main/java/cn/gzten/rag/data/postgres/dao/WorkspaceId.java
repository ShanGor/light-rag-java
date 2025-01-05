package cn.gzten.rag.data.postgres.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceId implements Serializable {
    @Column(columnDefinition = "varchar(255)")
    private String workspace;
    @Column(columnDefinition = "varchar(255)")
    private String id;
}
