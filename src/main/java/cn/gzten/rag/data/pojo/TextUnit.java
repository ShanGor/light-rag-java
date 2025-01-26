package cn.gzten.rag.data.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TextUnit {
    private String id;
    private TextChunk data;
    private int order;
    private int relationCounts;

    public void increaseRelationCounts() {
        this.relationCounts++;
    }
}
