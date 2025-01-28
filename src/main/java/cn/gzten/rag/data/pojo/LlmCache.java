package cn.gzten.rag.data.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmCache {
    private String workspace;
    private String id;
    private String mode;
    private String originalPrompt;
    private String returnValue;

    public static final LlmCache EMPTY = new LlmCache("", "", "", "", "");

    public boolean isEmpty() {
        return "".equals(this.workspace) && "".equals(this.id) && "".equals(this.mode);
    }
}
