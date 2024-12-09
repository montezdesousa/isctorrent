package com.isctorrent.logic.messages;

import java.io.Serializable;

public class FileSearchResult implements Serializable {

  private static final long serialVersionUID = 1L;

  private WordSearchMessage message;
  private String fileName;
  private long fileSize;
  private String fileHash;
  private String hostName;
  private int port;

  public FileSearchResult(
    WordSearchMessage message,
    String fileName,
    long fileSize,
    String fileHash,
    String hostName,
    int port
  ) {
    this.message = message;
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.fileHash = fileHash;
    this.hostName = hostName;
    this.port = port;
  }

  @Override
  public String toString() {
    return (
      "FileSearchResult{fileName='" +
      fileName +
      "'', fileSize=" +
      fileSize +
      ", fileHash='" +
      fileHash +
      "', hostName='" +
      hostName +
      "', port=" +
      port
    );
  }

  public WordSearchMessage getMessage() {
    return message;
  }

  public String getFileName() {
    return fileName;
  }

  public long getFileSize() {
    return fileSize;
  }

  public String getFileHash() {
    return fileHash;
  }

  public String getHostName() {
    return hostName;
  }

  public int getPort() {
    return port;
  }
}
