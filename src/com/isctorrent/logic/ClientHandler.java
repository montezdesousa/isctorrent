package com.isctorrent.logic;

import com.isctorrent.logic.messages.*;
import com.isctorrent.logic.models.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

class ClientHandler extends Thread {

  private final Connection connection;
  private final Node node;
  private final BlockingQueue<FileBlockRequestMessage> downloadTaskQueue;
  private final int N_THREADS = 5;
  private final ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);

  public ClientHandler(Connection connection, Node node) {
    this.connection = connection;
    this.node = node;
    this.downloadTaskQueue = new LinkedBlockingQueue<>();
    startDownloadWorkers();
  }

  private void startDownloadWorkers() {
    for (int i = 0; i < N_THREADS; i++) {
      pool.submit(() -> {
        while (true) {
          try {
            FileBlockRequestMessage request = downloadTaskQueue.take();
            processFileBlockRequest(request);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[ERROR] Task processing interrupted: " + e);
          }
        }
      });
    }
  }

  private void processFileBlockRequest(FileBlockRequestMessage request) {
    try {
      FileBlock block = node.getWorkFolder().getFileBlock(request);
      if (block == null) return;
      FileBlockAnswerMessage answer = new FileBlockAnswerMessage(
        request.getHash(),
        request.getOffset(),
        request.getLength(),
        request.getIndex(),
        block.getData()
      );
      connection.send(answer);
    } catch (Exception e) {
      System.out.println("[ERROR] Error processing file block request: " + e);
    }
  }

  private void handleFileBlockRequestMessage(FileBlockRequestMessage request) {
    try {
      downloadTaskQueue.put(request);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println(
        "[ERROR] Failed to add task to queue: " + e.getMessage()
      );
    }
  }

  private void handleNewConnectionRequest(NewConnectionRequest request) {
    String hostName = request.getHostName();
    int port = request.getPort();
    Node peer = new Node(hostName, port);
    node.addPeer(peer);
    NewConnectionRequestAck ack = new NewConnectionRequestAck(peer.getNodeId());
    connection.send(ack);
  }

  private void handleWordSearchMessage(WordSearchMessage message) {
    WorkFolder workFolder = node.getWorkFolder();
    List<FileMetadata> fileMetadata = workFolder.searchFilesByKeyword(
      message.getWord()
    );
    List<FileSearchResult> results = fileMetadata
      .stream()
      .map(metadata ->
        new FileSearchResult(
          message,
          metadata.getFileName(),
          metadata.getFileSize(),
          metadata.getFileHash(),
          node.getHostName(),
          node.getPort()
        )
      )
      .collect(Collectors.toList());
    connection.send(results);
  }

  @Override
  public void run() {
    Object object;
    while ((object = connection.receive()) != null) {
      if (object instanceof NewConnectionRequest) {
        handleNewConnectionRequest((NewConnectionRequest) object);
      } else if (object instanceof WordSearchMessage) {
        handleWordSearchMessage((WordSearchMessage) object);
      } else if (object instanceof FileBlockRequestMessage) {
        handleFileBlockRequestMessage((FileBlockRequestMessage) object);
      } else {
        System.out.println("[ERROR] Unrecognized object received: " + object);
      }
    }
  }
}
