package com.github.memcached.common.utils;

public enum Operation {
    SET((byte)0x01),
    GET((byte)0x10),
    DELETE((byte)0x11),
    SUCCESS((byte)0x7f),
    FAIL((byte)0x00),
    UNKNOWN((byte)-1);

    private byte code;

    Operation(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static Operation from(byte code) {
        if (code == SET.getCode()) {
            return SET;
        } else if (code == GET.getCode()) {
            return GET;
        } else if (code == DELETE.getCode()) {
            return DELETE;
        } else if (code == SUCCESS.getCode()) {
            return SUCCESS;
        } else if (code == FAIL.getCode()) {
            return FAIL;
        } else {
            return UNKNOWN;
        }
    }
}
