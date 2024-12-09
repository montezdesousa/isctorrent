package com.isctorrent.logic.models;

public class FileBlock {
    private long offset;
    private byte[] data;

    public FileBlock(long offset, byte[] data) {
        this.offset = offset;
        this.data = data;
    }

    public long getOffset() {
        return offset;
    }

    public byte[] getData() {
        return data;
    }
}