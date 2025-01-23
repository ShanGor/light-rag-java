package cn.gzten.rag.data.pojo;

public interface TextChunk {
    String getFullDocId();
    Integer getChunkOrderIndex();
    Integer getTokens();
    String getContent();
    float[] getContentVector();
}