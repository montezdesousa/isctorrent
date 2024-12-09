package com.isctorrent.logic;

import com.isctorrent.gui.Alert;
import com.isctorrent.logic.messages.*;
import com.isctorrent.logic.models.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {

  private InetAddress ipAddress;
  private String hostName;
  private int port;
  private NodeId nodeId;
  private ServerSocket serverSocket;
  private Map<NodeId, Connection> connections = new HashMap<>();
  private final long CONNECT_TIMEOUT = 2000;
  private WorkFolder workFolder = null;

  public Node(String hostName, int port) {
    this.port = port;
    try {
      this.ipAddress = InetAddress.getByName(hostName);
      this.hostName = hostName;
      this.nodeId = new NodeId(hostName, port);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Unknown host: " + hostName, e);
    }
  }

  @Override
  public String toString() {
    return "Node{nodeId='" + nodeId + "'}";
  }

  public NodeId getNodeId() {
    return nodeId;
  }

  public String getHostName() {
    return hostName;
  }

  public InetAddress getIpAddress() {
    return ipAddress;
  }

  public int getPort() {
    return port;
  }

  public void startServerAsync() {
    Thread serverThread = new Thread(() -> startServer());
    serverThread.start();
  }

  public void startServer() {
    try {
      serverSocket = new ServerSocket(port);
      System.out.println("[INFO] Node listening on port: " + port);
      while (!serverSocket.isClosed()) {
        Socket clientSocket = serverSocket.accept();
        Connection connection = new Connection(clientSocket);
        ClientHandler handler = new ClientHandler(connection, this);
        handler.start();
      }
    } catch (IOException e) {
      if (serverSocket != null && serverSocket.isClosed()) {
        System.out.println("[INFO] Server stopped.");
      } else {
        System.err.println("[ERROR] Server error message: " + e.getMessage());
      }
    }
  }

  public void addPeerAsync(Node peer) {
    Thread connectionThread = new Thread(() -> addPeer(peer));
    connectionThread.start();
  }

  public void addPeer(Node peer) {
    new Thread(() -> {
      try {
        Socket client = new Socket(peer.getIpAddress(), peer.getPort());
        synchronized (connections) {
          connections.put(peer.getNodeId(), new Connection(client));
          connections.notifyAll();
        }
        System.out.println("[INFO] Added new peer: " + peer);
      } catch (IOException e) {
        System.err.println("[ERROR] Failed to add peer: " + peer);
        synchronized (connections) {
          connections.notifyAll();
        }
        e.printStackTrace();
      }
    })
      .start();
  }

  public void removePeer(Node peer) {
    synchronized (connections) {
      Connection connection = connections.remove(peer.getNodeId());
      if (connection != null) {
        connection.close();
      }
    }
  }

  public void startConnection(Node peer) {
    synchronized (connections) {
      long startTime = System.currentTimeMillis();
      while (
        (
          !connections.containsKey(peer.getNodeId()) ||
          connections.get(peer.getNodeId()).getSocket().isClosed()
        )
      ) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= CONNECT_TIMEOUT) {
          Alert.showError("Connection timeout with peer: " + peer.getNodeId());
          return;
        }

        try {
          connections.wait(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }

    Connection connection = connections.get(peer.getNodeId());
    NewConnectionRequest request = new NewConnectionRequest(
      this.getHostName(),
      this.getPort()
    );
    connection.send(request);

    Object object;
    while ((object = connection.receive()) != null) {
      if (object instanceof NewConnectionRequestAck) {
        NewConnectionRequestAck ack = (NewConnectionRequestAck) object;
        if (ack.getNodeId().equals(this.nodeId)) {
          Alert.showInfo("Connection accepted by peer: " + peer.getNodeId());
          return;
        } else {
          Alert.showError("Connection rejected by peer: " + peer.getNodeId());
          connection.close();
        }
      }
    }
  }

  public void setWorkfolder(WorkFolder workFolder) {
    this.workFolder = workFolder;
  }

  public WorkFolder getWorkFolder() {
    return this.workFolder;
  }

  public Map<String, List<FileSearchResult>> searchWord(String word) {
    if (connections.isEmpty()) {
      Alert.showInfo("No peers connected, search ignored.");
      return new HashMap<>();
    }

    List<Thread> threads = new ArrayList<>();
    List<FileSearchResult> results = new ArrayList<>();

    synchronized (connections) {
      for (Map.Entry<NodeId, Connection> entry : connections.entrySet()) {
        NodeId peerNodeId = entry.getKey();
        Connection connection = entry.getValue();
        if (connection.isAlive()) {
          Thread thread = new Thread(() -> {
            WordSearchMessage message = new WordSearchMessage(
              word,
              this.getHostName(),
              this.getPort()
            );
            connection.send(message);
            Object object;
            while ((object = connection.receive()) != null) {
              if (object instanceof List<?>) {
                try {
                  @SuppressWarnings("unchecked")
                  List<FileSearchResult> resultList = (List<FileSearchResult>) object;
                  results.addAll(resultList);
                } catch (ClassCastException e) {
                  throw new IllegalStateException(
                    "Received list is not of type List<FileSearchResult>",
                    e
                  );
                }
              }
              return;
            }
            System.err.println(
              "[ERROR] Timeout waiting for peer: " + peerNodeId
            );
          });
          threads.add(thread);
          thread.start();
        } else {
          System.out.println("[INFO] Dead connection with peer: " + peerNodeId);
        }
      }
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Map<String, List<FileSearchResult>> resultsByFileHash = new HashMap<>();
    synchronized (results) {
      for (FileSearchResult result : results) {
        resultsByFileHash
          .computeIfAbsent(result.getFileHash(), k -> new ArrayList<>())
          .add(result);
      }
    }
    return resultsByFileHash;
  }

  public void downloadAsync(String fileName, List<FileSearchResult> results) {
    Thread downloadThread = new Thread(() -> download(fileName, results));
    downloadThread.start();
  }

  public void download(String fileName, List<FileSearchResult> results) {
    System.out.println("[INFO] Downloading " + results);
    try {
      DownloadTasksManagerWithLocks manager = new DownloadTasksManagerWithLocks(
        fileName,
        this.getWorkFolder()
      );
      manager.download(results, connections);
    } catch (Exception e) {
      Alert.showError("Error downloading file: " + fileName);
    }
  }
}
