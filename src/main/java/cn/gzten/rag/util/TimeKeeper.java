package cn.gzten.rag.util;

public class TimeKeeper {
    private long startTime;
    private TimeKeeper() {
        startTime = System.currentTimeMillis();
    }
    public static TimeKeeper start() {
        return new TimeKeeper();
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Milliseconds.
     */
    public long elapsed() {
        return System.currentTimeMillis() - startTime;
    }

    public double elapsedSeconds() {
        return elapsed() / 1000.0;
    }
}
