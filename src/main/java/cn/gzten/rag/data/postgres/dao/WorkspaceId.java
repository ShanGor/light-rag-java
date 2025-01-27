package cn.gzten.rag.data.postgres.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceId implements Serializable {
    private String workspace;
    private String id;
}
