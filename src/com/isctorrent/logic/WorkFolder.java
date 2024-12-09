package com.isctorrent.logic;

import com.isctorrent.gui.Alert;
import com.isctorrent.logic.messages.*;
import com.isctorrent.logic.models.*;
import com.isctorrent.util.Config;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkFolder {

  private final long BLOCK_SIZE = Config.BLOCK_SIZE;
  // Shared variables
  private File folder;
  // Hash -> FileMetadata
  private Map<String, FileMetadata> fileMetadataMap;
  // Hash -> Offset -> FileBlock
  private Map<String, Map<Long, FileBlock>> fileBlocksCache;

  public WorkFolder(String path) {
    folder = new File(path);
    fileMetadataMap = new HashMap<>();
    fileBlocksCache = new HashMap<>();
    refreshFileMetadataMap();
  }

  private static String appendTimestampToFileName(
    String fileName,
    String timestamp
  ) {
    int dotIndex = fileName.lastIndexOf(".");
    if (dotIndex != -1) {
      return (
        fileName.substring(0, dotIndex) +
        "_" +
        timestamp +
        fileName.substring(dotIndex)
      );
    } else {
      return fileName + "_" + timestamp;
    }
  }

  private static String getCurrentTimestamp() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
      "yyyyMMdd_HHmmss"
    );
    return now.format(formatter);
  }

  public String getTimeStampedFilePath(String fileName) {
    String timestamp = getCurrentTimestamp();
    String timestampedFileName = appendTimestampToFileName(fileName, timestamp);
    String path = folder.getPath() + File.separator + timestampedFileName;
    return path;
  }

  public void writeFileAndUpdateFileMetadataMap(
    String path,
    List<byte[]> dataBlocks
  ) {
    File newFile = new File(path);
    try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {
      dataBlocks.forEach(block -> {
        try {
          fileOutputStream.write(block);
        } catch (IOException e) {
          Alert.showError("Failed to write file: " + path);
        }
      });
      // Update fileMetadataMap
      synchronized (fileMetadataMap) {
        FileMetadata newFileMetadata = new FileMetadata(newFile);
        fileMetadataMap.put(newFileMetadata.getFileHash(), newFileMetadata);
      }
      System.out.println("File written successfully to " + path);
    } catch (IOException e) {
      System.err.println("Error writing the file: " + e.getMessage());
    }
  }

  public void refreshFileMetadataMap() {
    synchronized (fileMetadataMap) {
      if (folder.exists() && folder.isDirectory()) {
        File[] files = folder.listFiles(File::isFile);
        for (File file : files) {
          FileMetadata fileMetadata = new FileMetadata(file);
          fileMetadataMap.put(fileMetadata.getFileHash(), fileMetadata);
        }
      } else {
        System.out.println(
          "The directory is no longer valid: " + folder.getPath()
        );
      }
    }
  }

  public List<FileMetadata> searchFilesByKeyword(String keyword) {
    keyword = keyword.toLowerCase();
    List<FileMetadata> result = new ArrayList<>();
    synchronized (fileMetadataMap) {
      for (Map.Entry<String, FileMetadata> entry : this.fileMetadataMap.entrySet()) {
        FileMetadata fileMetadata = entry.getValue();
        if (fileMetadata.getFileName().contains(keyword)) {
          result.add(entry.getValue());
        }
      }
    }
    return result;
  }

  public synchronized FileBlock getFileBlock(FileBlockRequestMessage request) {
    String hash = request.getHash();
    long offset = request.getOffset();
    synchronized (fileBlocksCache) {
      Map<Long, FileBlock> fileBlocksMap = this.fileBlocksCache.get(hash);
      if (fileBlocksMap != null) return fileBlocksMap.get(offset);
      synchronized (fileMetadataMap) {
        // Cache miss
        FileMetadata fileMetadata = this.fileMetadataMap.get(hash);
        if (fileMetadata != null) {
          Map<Long, FileBlock> newFileBlocksMap = getFileBlocksMap(
            fileMetadata
          );
          this.fileBlocksCache.put(hash, newFileBlocksMap);
          return newFileBlocksMap.get(offset);
        }
      }
    }
    System.out.println("[ERROR] Hash not found: " + hash);
    return null;
  }

  private synchronized Map<Long, FileBlock> getFileBlocksMap(
    FileMetadata fileMetadata
  ) {
    Map<Long, FileBlock> newFileBlocksMap = new HashMap<>();
    File file = fileMetadata.getFile();
    RandomAccessFile randomAccessFile = null;
    try {
      randomAccessFile = new RandomAccessFile(file, "r");
      FileChannel channel = randomAccessFile.getChannel();
      long size = channel.size();
      long offset = 0;
      while (offset < size) {
        int length = (int) Math.min(BLOCK_SIZE, size - offset);
        ByteBuffer buffer = ByteBuffer.allocate(length);
        channel.position(offset);
        channel.read(buffer);
        FileBlock block = new FileBlock(offset, buffer.array());
        newFileBlocksMap.put(offset, block);
        offset += BLOCK_SIZE;
      }
    } catch (Exception e) {
      System.out.println("[ERROR] Failed to load file: " + file.getName());
      return null;
    } finally {
      if (randomAccessFile != null) try {
        randomAccessFile.close();
      } catch (IOException e) {
        System.out.println("[ERROR] Failed to close file: " + file.getName());
      }
    }
    return newFileBlocksMap;
  }
}
