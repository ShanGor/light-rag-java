package cn.gzten.rag.llm;

public interface EmbeddingFunc {
    int getDimension();
    int getMaxTokenSize();
    int getConcurrentLimit();
    float[] convert(String input);
}
