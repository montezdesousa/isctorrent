package com.isctorrent.logic.messages;

import com.isctorrent.logic.models.*;
import java.io.Serializable;

public class NewConnectionRequestAck implements Serializable {

  private static final long serialVersionUID = 1L;
  private NodeId nodeId;

  public NewConnectionRequestAck(NodeId nodeId) {
    this.nodeId = nodeId;
  }

  public NodeId getNodeId() {
    return nodeId;
  }

  @Override
  public String toString() {
    return "NewConnectionRequestAck{nodeId='" + nodeId + "}";
  }
}
