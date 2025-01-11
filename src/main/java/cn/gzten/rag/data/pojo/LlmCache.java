package cn.gzten.rag.data.pojo;

import jakarta.persistence.Column;

public interface LlmCache {
    String getOriginalPrompt();
    String getReturnValue();
}
