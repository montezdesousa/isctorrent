package com.isctorrent.logic.messages;

import java.io.Serializable;

public class FileBlockAnswerMessage implements Serializable{

    private static final long serialVersionUID = 1L;
    private String hash;
    private long offset;
    private long length;
    private int index;
    private byte[] data;

    public FileBlockAnswerMessage(String hash, long offset, long length, int index, byte[] data) {
        this.hash = hash;
        this.offset = offset;
        this.length = length;
        this.index = index;
        this.data = data;
    }

    public String getHash() {
        return hash;
    }

    public long getOffset() {
        return offset;
    }

    public long getLength() {
        return length;
    }

    public int getIndex() {
        return index;
    }

    public byte[] getData() {
        return data;
    }
  
    @Override
    public String toString() {
      return "FileBlockAnswerMessage{" +
        "hash='" + hash + '\'' +
        ", offset=" + offset +
        ", length=" + length +
        ", index=" + index +
        '}';
    }

}
