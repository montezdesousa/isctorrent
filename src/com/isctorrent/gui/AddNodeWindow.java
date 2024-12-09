package com.isctorrent.gui;

import com.isctorrent.logic.Node;

import java.awt.*;

import javax.swing.*;

public class AddNodeWindow {

  public static void open(Node thisNode) {
    JFrame connectFrame = new JFrame("Connect to Node");
    connectFrame.setSize(300, 200);
    connectFrame.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 10, 10, 10);

    JLabel addressLabel = new JLabel("Address:");
    JTextField addressField = new JTextField("localhost");
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    connectFrame.add(addressLabel, gbc);
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    connectFrame.add(addressField, gbc);

    JLabel portLabel = new JLabel("Port:");
    JTextField portField = new JTextField("8082");
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.NONE;
    connectFrame.add(portLabel, gbc);
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    connectFrame.add(portField, gbc);

    JPanel buttonPanel = new JPanel();
    JButton cancelButton = new JButton("Cancel");
    JButton okButton = new JButton("OK");
    buttonPanel.add(cancelButton);
    buttonPanel.add(okButton);

    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.CENTER;
    connectFrame.add(buttonPanel, gbc);

    // Action listeners for the buttons
    cancelButton.addActionListener(e -> connectFrame.dispose());
    okButton.addActionListener(e -> {
      String address = addressField.getText();
      String port = portField.getText();

      try {
        int portNumber = Integer.parseInt(port);
        if (thisNode.getPort() == portNumber) {
          Alert.showError("Cannot connect to self.");
          return;
        }
        Node peer = new Node(address, portNumber);
        thisNode.addPeerAsync(peer);
        thisNode.startConnection(peer);
        connectFrame.dispose();
      } catch (NumberFormatException ex) {
        Alert.showError("Invalid port number. Please enter a numeric value.");
      }
    });

    connectFrame.setVisible(true);
  }
}
