package cn.gzten.rag.data.storage.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static cn.gzten.rag.util.LightRagUtils.objectToJsonSnake;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagGraphNode {
    protected long id;
    protected String label;
    @JsonProperty("node_id")
    protected String nodeId;
    @JsonProperty("source_id")
    protected String sourceId;
    @JsonProperty("entity_type")
    protected String entityType;
    protected String description;

    public String toGraphProperties() {
        return "{node_id:%s,source_id:%s,entity_type:%s,description:%s}"
                .formatted(objectToJsonSnake(nodeId),
                        objectToJsonSnake(sourceId),
                        objectToJsonSnake(entityType),
                        objectToJsonSnake(description));
    }

    public static final RagGraphNode EMPTY = new RagGraphNode(-6, "", "", "", "", "");

    public boolean isEmpty() {
        return id == -6;
    }
}
