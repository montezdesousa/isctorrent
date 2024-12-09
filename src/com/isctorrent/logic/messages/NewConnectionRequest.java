package com.isctorrent.logic.messages;

import java.io.Serializable;

public class NewConnectionRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String hostName;
    private int port;

    public NewConnectionRequest(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "NewConnectionRequest{hostName='" + hostName + "', port=" + port + "}";
    }
}

