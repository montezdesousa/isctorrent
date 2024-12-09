package com.isctorrent.logic;

import com.isctorrent.gui.Alert;
import com.isctorrent.logic.messages.*;
import com.isctorrent.logic.models.*;
import com.isctorrent.util.Config;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadTasksManagerWithLocks {

  private static final int TASK_QUEUE_TIMEOUT = 5; // seconds
  private static final int POOL_TERMINATION_TIMEOUT = 60; // seconds
  private static final long BLOCK_SIZE = Config.BLOCK_SIZE;
  private final String fileName;
  private final WorkFolder workFolder;
  private List<FileBlockRequestMessage> requests;
  // Shared variables
  private List<FileBlockAnswerMessage> answers;
  private Map<NodeId, Integer> numBlocksByPeer;

  private final Lock answersLock;
  private final Lock numBlocksByPeerLock;
  private final Lock writerLock;
  private final Condition writerCondition;

  private int pendingTasks;

  public DownloadTasksManagerWithLocks(String fileName, WorkFolder workFolder) {
    this.fileName = fileName;
    this.workFolder = workFolder;
    this.requests = new ArrayList<>();
    this.answers = new ArrayList<>();
    this.numBlocksByPeer = new HashMap<>();

    this.answersLock = new ReentrantLock();
    this.numBlocksByPeerLock = new ReentrantLock();
    this.writerLock = new ReentrantLock();
    this.writerCondition = writerLock.newCondition();

    this.pendingTasks = 0;
  }

  public String getFileName() {
    return fileName;
  }

  public WorkFolder getWorkFolder() {
    return workFolder;
  }

  public List<FileBlockRequestMessage> getRequests() {
    return requests;
  }

  public List<FileBlockRequestMessage> createRequestList(
    String fileHash,
    long fileSize
  ) {
    List<FileBlockRequestMessage> requests = new ArrayList<>();
    for (long offset = 0; offset < fileSize; offset += BLOCK_SIZE) {
      int currentBlockSize = (int) Math.min(BLOCK_SIZE, fileSize - offset);
      int blockIndex = (int) (offset / BLOCK_SIZE);
      FileBlockRequestMessage request = new FileBlockRequestMessage(
        fileHash,
        offset,
        currentBlockSize,
        blockIndex
      );
      requests.add(request);
    }
    return requests;
  }

  public void download(
    List<FileSearchResult> results,
    Map<NodeId, Connection> connections
  ) {
    List<FileBlockRequestMessage> requests = prepareRequests(results);
    writerLock.lock();
    try {
      this.pendingTasks = requests.size();
    } finally {
      writerLock.unlock();
    }

    startWriterThread(requests, System.currentTimeMillis());
    dispatchTasks(results, connections, requests);
  }

  private List<NodeId> getPeerIdsWithFile(List<FileSearchResult> results) {
    List<NodeId> peers = new ArrayList<>();
    for (FileSearchResult result : results) {
      peers.add(new NodeId(result.getHostName(), result.getPort()));
    }
    return peers;
  }

  private List<FileBlockRequestMessage> prepareRequests(
    List<FileSearchResult> results
  ) {
    String fileHash = results.get(0).getFileHash();
    long fileSize = results.get(0).getFileSize();
    return createRequestList(fileHash, fileSize);
  }

  private void startWriterThread(
    List<FileBlockRequestMessage> requests,
    long startTime
  ) {
    new Thread(() -> {
      writerLock.lock();
      try {
        while (pendingTasks > 0) {
          writerCondition.await();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.err.println("[ERROR] File writing thread interrupted.");
        return;
      } finally {
        writerLock.unlock();
      }

      answersLock.lock();
      try {
        int missingBlocks = requests.size() - answers.size();
        if (missingBlocks > 0) {
          Alert.showError(
            "Download failed: " + missingBlocks + " blocks missing."
          );
          return;
        }
        Collections.sort(
          answers,
          (a, b) -> Long.compare(a.getOffset(), b.getOffset())
        );
        String path = workFolder.getTimeStampedFilePath(fileName);
        workFolder.writeFileAndUpdateFileMetadataMap(
          path,
          answers.stream().map(answer -> answer.getData()).toList()
        );
        long endTime = System.currentTimeMillis();
        showCompletionWindow(path, endTime - startTime);
      } finally {
        answersLock.unlock();
      }
    })
      .start();
  }

  private void dispatchTasks(
    List<FileSearchResult> results,
    Map<NodeId, Connection> connections,
    List<FileBlockRequestMessage> requests
  ) {
    BlockingQueue<FileBlockRequestMessage> taskQueue = new LinkedBlockingQueue<>(
      requests
    );

    List<NodeId> peerIds = getPeerIdsWithFile(results);
    ExecutorService pool = Executors.newFixedThreadPool(peerIds.size());

    for (NodeId peerId : peerIds) {
      pool.submit(() -> processTasks(peerId, connections.get(peerId), taskQueue)
      );
    }
    pool.shutdown();
    try {
      if (!pool.awaitTermination(POOL_TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
        pool.shutdownNow();
      }
    } catch (InterruptedException e) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
      System.err.println("[ERROR] Thread pool termination interrupted.");
    }
  }

  private void processTasks(
    NodeId peerId,
    Connection connection,
    BlockingQueue<FileBlockRequestMessage> taskQueue
  ) {
    while (true) {
      FileBlockRequestMessage request = null;
      try {
        request = taskQueue.poll(TASK_QUEUE_TIMEOUT, TimeUnit.SECONDS);
        if (request == null) break;
        connection.send(request);
        Object response;
        while ((response = connection.receive()) != null) {
          if (response instanceof FileBlockAnswerMessage) {
            answersLock.lock();
            try {
              answers.add((FileBlockAnswerMessage) response);
            } finally {
              answersLock.unlock();
            }

            numBlocksByPeerLock.lock();
            try {
              numBlocksByPeer.put(
                peerId,
                numBlocksByPeer.getOrDefault(peerId, 0) + 1
              );
            } finally {
              numBlocksByPeerLock.unlock();
            }
          }
          break;
        }
      } catch (InterruptedException e) {
        System.out.println("[ERROR] Interrupted: " + e.getMessage());
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        System.out.println(
          "[ERROR] Error during task processing: " + e.getMessage()
        );
      } finally {
        if (request != null) {
          writerLock.lock();
          try {
            pendingTasks--;
            if (pendingTasks == 0) {
              writerCondition.signalAll();
            }
          } finally {
            writerLock.unlock();
          }
        }
      }
    }
  }

  private void showCompletionWindow(String filePath, long timeSpent) {
    StringBuilder message = new StringBuilder(
      "Download complete: " + filePath + "\n"
    );
    numBlocksByPeerLock.lock();
    try {
      numBlocksByPeer.forEach((peerNodeId, count) -> {
        message
          .append("Provider ")
          .append(peerNodeId)
          .append(": ")
          .append(count)
          .append(" blocks\n");
      });
    } finally {
      numBlocksByPeerLock.unlock();
    }
    message.append("Time elapsed: ").append(timeSpent / 1000.0).append("s\n");
    Alert.showInfo(message.toString());
  }
}
