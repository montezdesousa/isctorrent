package com.isctorrent.logic.models;

import java.io.Serializable;
import java.util.Objects;

public class NodeId implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String hostName;
    private final int port;
    private final String id;

    public NodeId(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
        this.id = hostName + ":" + port;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "[address=" + hostName + ":port=" + port + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NodeId other = (NodeId) obj;
        return this.id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    } 
}
