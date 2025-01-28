package cn.gzten.rag.data.storage.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static cn.gzten.rag.util.LightRagUtils.objectToJsonSnake;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagGraphEdge {
    protected double weight;
    protected String keywords;
    protected String sourceId;
    protected String description;

    public String toGraphProperties() {
        return "{weight:%f,keywords:%s,source_id:%s,description:%s}"
                .formatted(weight,
                        objectToJsonSnake(keywords),
                        objectToJsonSnake(sourceId),
                        objectToJsonSnake(description));
    }

    public static final RagGraphEdge EMPTY = new RagGraphEdge(0, "", "", "");

    public boolean isEmpty() {
        return "".equals(keywords) && "".equals(sourceId) && "".equals(description);
    }
}
