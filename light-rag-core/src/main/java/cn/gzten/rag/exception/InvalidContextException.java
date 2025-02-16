package cn.gzten.rag.exception;

public class InvalidContextException extends RuntimeException{
    public InvalidContextException(String message) {
        super(message);
    }
}
