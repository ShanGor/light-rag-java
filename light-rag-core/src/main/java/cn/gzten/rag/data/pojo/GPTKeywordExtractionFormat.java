package cn.gzten.rag.data.pojo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GPTKeywordExtractionFormat {
    @JsonAlias("high_level_keywords")
    private List<String> highLevelKeywords;
    @JsonAlias("low_level_keywords")
    private List<String> lowLevelKeywords;
}
