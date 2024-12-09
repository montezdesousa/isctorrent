package com.isctorrent.gui;

import com.isctorrent.logic.Node;
import com.isctorrent.logic.WorkFolder;
import com.isctorrent.logic.messages.FileSearchResult;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class MainWindow {

  public static final int GUI_WIDTH = 600;
  public static final int GUI_HEIGHT = 400;

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("[ERROR] Usage: java IscTorrent <port> <folder>");
      System.exit(1);
    }
    int port;
    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      System.out.println("[ERROR] Port number must be an integer.");
      System.exit(1);
      return;
    }

    // Validate folder
    String folderPath = args[1];
    File folder = new File(folderPath);
    if (!folder.exists() || !folder.isDirectory()) {
      System.out.println(
        "[ERROR] The specified folder does not exist or is not a directory."
      );
      System.exit(1);
    }

    WorkFolder workFolder = new WorkFolder(folderPath);
    Node thisNode = new Node("localhost", port);
    thisNode.setWorkfolder(workFolder);
    thisNode.startServerAsync();

    JFrame frame = new JFrame(
      "Port NodeAddress [address=localhost" + ", port=" + port + "]"
    );
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(GUI_WIDTH, GUI_HEIGHT);

    frame.setLayout(new BorderLayout());

    // Search panel
    JPanel searchPanel = new JPanel();
    searchPanel.setLayout(new BorderLayout());
    JLabel searchLabel = new JLabel("Texto a procurar:     ");
    searchPanel.add(searchLabel, BorderLayout.WEST);
    JTextField searchField = new JTextField("");
    searchPanel.add(searchField, BorderLayout.CENTER);
    JButton searchButton = new JButton("Procurar");
    searchButton.setPreferredSize(new Dimension(180, 30));
    DefaultListModel<String> model = new DefaultListModel<>();
    // fileName.txt <2> : [FileSearchResult_Node_1, FileSearchResult_Node_2]
    Map<String, List<FileSearchResult>> resultsMap = new HashMap<>();
    searchButton.addActionListener(e -> {
      model.clear();
      resultsMap.clear();
      String word = searchField.getText();
      Map<String, List<FileSearchResult>> resultsByFileHash = thisNode.searchWord(
        word
      );
      for (Map.Entry<String, List<FileSearchResult>> entry : resultsByFileHash.entrySet()) {
        // fileHash: [FileSearchResult, ...]
        List<FileSearchResult> fileResults = entry.getValue();
        String fileName = fileResults.get(0).getFileName();
        int count = fileResults.size();
        String displayName = fileName + " <" + count + ">";
        model.addElement(displayName);
        resultsMap.put(displayName, fileResults);
      }
    });
    searchPanel.add(searchButton, BorderLayout.EAST);
    frame.add(searchPanel, BorderLayout.NORTH);

    // Results panel
    JList<String> resultsList = new JList<>(model);
    resultsList.setSelectionMode(
      ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    );
    JScrollPane resultsScrollPane = new JScrollPane(resultsList);
    frame.add(resultsScrollPane, BorderLayout.CENTER);

    // Buttons panel
    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    // Download
    JButton downloadButton = new JButton("Download");
    downloadButton.setPreferredSize(new Dimension(150, 100));
    downloadButton.addActionListener(e -> {
      List<String> values = resultsList.getSelectedValuesList();
      for (String value : values) {
        List<FileSearchResult> results = resultsMap.get(value);
        if (results != null && !results.isEmpty()) {
          String fileName = results.get(0).getFileName();
          thisNode.downloadAsync(fileName, results);
        }
      }
    });
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    buttonsPanel.add(downloadButton, gbc);

    // Add node
    JButton addNodeButton = new JButton("Add Node");
    addNodeButton.setPreferredSize(new Dimension(150, 100));
    gbc.gridy = 1;
    buttonsPanel.add(addNodeButton, gbc);
    addNodeButton.addActionListener(e -> AddNodeWindow.open(thisNode));
    frame.add(buttonsPanel, BorderLayout.EAST);

    frame.setVisible(true);
  }
}
