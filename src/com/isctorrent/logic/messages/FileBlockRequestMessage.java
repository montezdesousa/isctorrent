package com.isctorrent.logic.messages;

import java.io.Serializable;

public class FileBlockRequestMessage implements Serializable {

  private static final long serialVersionUID = 1L;
  private String hash;
  private long offset;
  private long length;
  private int index;

  public FileBlockRequestMessage(String hash, long offset, long length, int index) {
    this.hash = hash;
    this.offset = offset;
    this.length = length;
    this.index = index;
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

  @Override
  public String toString() {
    return (
      "FileBlockRequestMessage{" +
      "hash='" +
      hash +
      "', offset=" +
      offset +
      ", length=" +
      length +
      ", index=" +
      index +
      '}'
    );
  }
}
