package cn.gzten.rag.data.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Optional;

@Data
@Builder
public class TextUnit {
    private String id;
    private Optional<TextChunk> data;
    private int order;
    private int relationCounts;

    public void increaseRelationCounts() {
        this.relationCounts++;
    }
}
