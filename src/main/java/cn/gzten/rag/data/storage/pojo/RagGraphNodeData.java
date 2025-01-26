package cn.gzten.rag.data.storage.pojo;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagGraphNodeData extends RagGraphNode{
    private String entityName;
    private int rank;
    public RagGraphNodeData(RagGraphNode node) {
        super(node.getId(), node.getLabel(), node.getNodeId(), node.getSourceId(), node.getEntityType(), node.getDescription());
    }
}
