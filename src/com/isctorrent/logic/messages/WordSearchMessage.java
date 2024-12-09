package com.isctorrent.logic.messages;

import java.io.Serializable;

public class WordSearchMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  private String word;
  private String senderIpAddress;
  private int senderPort;

  public WordSearchMessage(
    String word,
    String senderIpAddress,
    int senderPort
  ) {
    this.word = word;
    this.senderIpAddress = senderIpAddress;
    this.senderPort = senderPort;
  }

  public String getWord() {
    return word;
  }

  public String getSenderIpAddress() {
    return senderIpAddress;
  }

  public int getSenderPort() {
    return senderPort;
  }

  @Override
  public String toString() {
    return (
      "WordSearchMessage{word='" +
      word +
      "', sender=" +
      senderIpAddress +
      ":" +
      senderPort +
      "}"
    );
  }
}
