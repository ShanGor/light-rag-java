package cn.gzten.rag.data.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryContext {
    private String entitiesContext;
    private String relationsContext;
    private String textUnitsContext;
}
