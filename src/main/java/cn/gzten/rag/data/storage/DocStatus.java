package cn.gzten.rag.data.storage;

import lombok.Getter;

@Getter
public enum DocStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    PROCESSED("processed"),
    FAILED("failed");

    private final String status;

    DocStatus(String status) {
        this.status = status;
    }
}
