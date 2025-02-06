package cn.gzten.rag.data.storage.pojo;

import cn.gzten.rag.data.pojo.NullablePair;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagGraphEdgeData extends RagGraphEdge {
    public RagGraphEdgeData(RagGraphEdge edge) {
        super(edge.getWeight(), edge.getKeywords(), edge.getSourceId(), edge.getDescription());
    }
    private NullablePair<String, String> sourceTarget;
    private int rank;
}
