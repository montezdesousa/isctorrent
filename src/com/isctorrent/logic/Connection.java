package com.isctorrent.logic;

import java.io.*;
import java.net.Socket;

class Connection {
    private final Socket socket;
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectInputStream getInputStream() {
        return inputStream;
    }

    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    public void send(Object object) {
        synchronized (outputStream) {
            try {
                outputStream.writeObject(object);
                outputStream.flush();
                outputStream.reset();
                System.out.println("[INFO] Sent: " + object);
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to send: " + object);
            }
        }
    }

    public Object receive() {
        synchronized (inputStream) {
            try {
                Object object = inputStream.readObject();
                System.out.println("[INFO] Received: " + object);
                return object;
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("[ERROR] Error reading object: " + e.getMessage());
                return null;
            }
        }
    }

    public synchronized boolean isAlive() {
        return socket != null && !socket.isClosed();
    }

    public synchronized void close() {
        try {
            if (inputStream != null)
                inputStream.close();
            if (outputStream != null)
                outputStream.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Error closing SocketWrapper: " + e.getMessage());
        }
    }
}
