package com.github.memcached.common;

import com.github.memcached.common.utils.Operation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Request {
    // 请求的操作类型
    private byte operation;
    // 请求的key的长度
    private int keyLength;
    // 请求的key
    private String key;
    // 请求的value的长度
    private int valueLength;
    // 请求的value的值
    private String value;
    // key存活的时长
    private int ttl;

    public byte getOperation() {
        return operation;
    }

    public void setOperation(byte operation) {
        this.operation = operation;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getValueLength() {
        return valueLength;
    }

    public void setValueLength(int valueLength) {
        this.valueLength = valueLength;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    /**
     * 从流中读取数据
     *
     * @param dataInputStream
     * @throws IOException
     */
    public void readFrom(DataInputStream dataInputStream) throws IOException {
        this.operation = dataInputStream.readByte();

        this.keyLength = dataInputStream.readInt();
        byte[] keyBytes = new byte[keyLength];
        dataInputStream.read(keyBytes);
        this.key = new String(keyBytes);

        this.valueLength = dataInputStream.readInt();
        if(valueLength != -1) {
            byte[] valueBytes = new byte[valueLength];
            dataInputStream.read(valueBytes);
            this.value = new String(valueBytes);
        }
        this.ttl = dataInputStream.readInt();
    }

    /**
     * 将数据写入流中
     *
     * @param dataOutputStream
     * @throws IOException
     */
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(this.operation);

        byte[] keyBytes = this.key.getBytes();
        this.keyLength = keyBytes.length;
        dataOutputStream.writeInt(this.keyLength);
        dataOutputStream.write(keyBytes);

        if(this.value != null) {
            byte[] valueBytes = this.value.getBytes();
            this.valueLength = valueBytes.length;
            dataOutputStream.writeInt(this.valueLength);
            dataOutputStream.write(valueBytes);
        } else {
            dataOutputStream.writeInt(0);
            dataOutputStream.write(new byte[0]);
        }

        dataOutputStream.writeInt(ttl);
        dataOutputStream.flush();
    }

    @Override
    public String toString() {
        return "Request{" +
                "operation=" + Operation.from(operation) +
                ", keyLength=" + keyLength +
                ", key='" + key + '\'' +
                ", valueLength=" + valueLength +
                ", value='" + value + '\'' +
                ", ttl=" + ttl +
                '}';
    }
}
