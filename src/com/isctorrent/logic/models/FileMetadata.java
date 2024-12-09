package com.isctorrent.logic.models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileMetadata {

  private static final String HASH_ALGORITHM = "SHA-256";
  private final File file;
  private String fileName;
  private long fileSize;
  private String fileHash;

  public FileMetadata(File file) {
    this.file = file;
    this.fileName = file.getName();
    this.fileSize = file.length();
    try {
      this.fileHash = computeHash(file.getAbsolutePath(), HASH_ALGORITHM);
    } catch (Exception e) {
      throw new RuntimeException(
        "Error computing file hash: " + e.getMessage(),
        e
      );
    }
  }

  @Override
  public String toString() {
    return (
      "FileData{fileName='" +
      fileName +
      "'', fileSize=" +
      fileSize +
      ", fileHash='" +
      fileHash +
      "'}"
    );
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

  public File getFile() {
    return file;
  }

  public static String computeHash(String path, String algorithm)
    throws NoSuchAlgorithmException, FileNotFoundException, IOException {
    MessageDigest digest;
    digest = MessageDigest.getInstance(algorithm);
    try (FileInputStream fis = new FileInputStream(path)) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        digest.update(buffer, 0, bytesRead);
      }
    }
    StringBuilder hexString = new StringBuilder();
    byte[] hashBytes = digest.digest();
    for (byte b : hashBytes) {
      hexString.append(String.format("%02x", b));
    }
    return hexString.toString();
  }
}
