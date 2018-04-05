package com.github.memcached.common;

import com.github.memcached.common.utils.Operation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Response {
    // 响应的操作类型
    private byte operation;
    // 消息体的字节长度
    private int messageLength;
    // 消息体数据
    private byte[] message;

    public Response() {
    }

    public byte getOperation() {
        return operation;
    }

    public void setOperation(byte operation) {
        this.operation = operation;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    /**
     * 将数据写入流中
     * @param dataOutputStream
     * @throws IOException
     */
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(operation);
        dataOutputStream.writeInt(messageLength);
        dataOutputStream.write(message);
        dataOutputStream.flush();
    }

    /**
     * 从流中读取数据
     *
     * @param dataInputStream
     * @throws IOException
     */
    public void readFrom(DataInputStream dataInputStream) throws IOException {
        this.operation = dataInputStream.readByte();
        this.messageLength = dataInputStream.readInt();
        byte[] bytes = new byte[this.messageLength];
        dataInputStream.read(bytes);
        this.message = bytes;
    }

    @Override
    public String toString() {
        return "Response{" +
                "operation=" + Operation.from(operation) +
                ", messageLength=" + messageLength +
                ", message=" + new String(message) +
                '}';
    }
}
