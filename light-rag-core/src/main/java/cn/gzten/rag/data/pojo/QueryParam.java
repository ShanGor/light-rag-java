package cn.gzten.rag.data.pojo;

import lombok.Data;

@Data
public class QueryParam {
    private QueryMode mode = QueryMode.GLOBAL;
    private boolean onlyNeedContext = false;
    private boolean onlyNeedPrompt = false;
    private String responseType = "Multiple Paragraphs";
    private boolean stream = false;
    private int topK = 60;
    // Number of document chunks to retrieve.
    // top_n: int = 10
    // Number of tokens for the original chunks.
    private int maxTokenForTextUnit = 4000;
            // Number of tokens for the relationship descriptions
    private int maxTokenForGlobalContext = 4000;
            // Number of tokens for the entity descriptions
    private int maxTokenForLocalContext= 4000;
}
