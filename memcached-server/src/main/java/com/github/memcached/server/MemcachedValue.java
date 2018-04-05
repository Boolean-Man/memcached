package com.github.memcached.server;

public class MemcachedValue {
    private String value;

    private long expiredTimestamp;

    public MemcachedValue(String value, long expiredTimestamp) {
        this.value = value;
        this.expiredTimestamp = expiredTimestamp;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getExpiredTimestamp() {
        return expiredTimestamp;
    }

    public void setExpiredTimestamp(long expiredTimestamp) {
        this.expiredTimestamp = expiredTimestamp;
    }

    @Override
    public String toString() {
        return "MemcachedValue{" +
                "value='" + value + '\'' +
                ", expiredTimestamp=" + expiredTimestamp +
                '}';
    }
}
