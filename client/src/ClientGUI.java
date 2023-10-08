import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientGUI {
    private Client client;
    private JFrame frame;
    private JButton[][] boardButtons;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private JButton quitButton;
    private JLabel statusLabel;
    private JLabel playerInfoLabel;
    private Timer timer;
    private JLabel timerLabel;
    private JFrame preGameFrame;
    private JTextField playerNameInput;
    private JButton connectButton;
    private JLabel errorMessageLabel;
    private JFrame waitingFrame;
    private JLabel waitingLabel;
    private JPanel topPanel;
    private JLabel rankLabel;
    private JLabel turnLabel;
    private JScrollPane chatScrollPane;
    private DefaultListModel<String> chatListModel;
    private JList<String> chatList;
    private JTextField chatInputField;
    private JButton sendChatButton;

    public ClientGUI(Client client) {
        this.client = client;
        createPreGameFrame();
    }

    private void createPreGameFrame() {
        preGameFrame = new JFrame("Enter Player Name");
        preGameFrame.setLayout(new FlowLayout());
        playerNameInput = new JTextField(20);
        connectButton = new JButton("Connect");
        errorMessageLabel = new JLabel("");
        preGameFrame.add(playerNameInput);
        preGameFrame.add(connectButton);
        preGameFrame.add(errorMessageLabel);
        preGameFrame.pack();
        preGameFrame.setVisible(true);

        connectButton.addActionListener(e -> {
            String playerName = playerNameInput.getText();
            if (!playerName.isEmpty()) {
                boolean success = client.connectToServer(playerName);
                if (success) {
                    preGameFrame.dispose();
                    showWaitingScreen();
                } else {
                    errorMessageLabel.setText("Name already in use!");
                }
            }
        });
    }

    public void displayError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    public void updateRankAndName(String playerName, int rank, char symbol) {
        rankLabel.setText("Rank #" + rank + " '" + playerName + "' (" + symbol + ")");
    }

    public void updatePlayerTurn(String playerName) {
        turnLabel.setText("Player '" + playerName + "'s Turn");
    }


    public void createGameFrame() {
        if (waitingFrame != null) {
            waitingFrame.dispose();
        }

        frame = new JFrame("Tic Tac Toe");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Timer
        timerLabel = new JLabel("20s");
        frame.add(timerLabel, BorderLayout.NORTH);

        // Tic Tac Toe board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3));
        boardButtons = new JButton[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j] = new JButton("");
                boardButtons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
                boardPanel.add(boardButtons[i][j]);
            }
        }
        frame.add(boardPanel, BorderLayout.CENTER);

        // Player Info and Turn Info
        topPanel = new JPanel(new BorderLayout());  // 使用边框布局来组织子组件

        rankLabel = new JLabel("Rank #50 'username' (X)");  // 假设的初始值
        rankLabel.setHorizontalAlignment(JLabel.CENTER);

        turnLabel = new JLabel("Player 'username's Turn");
        turnLabel.setHorizontalAlignment(JLabel.CENTER);

        topPanel.add(rankLabel, BorderLayout.NORTH);
        topPanel.add(turnLabel, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);


        // Chat window
        chatListModel = new DefaultListModel<>();
        chatList = new JList<>(chatListModel);
        chatScrollPane = new JScrollPane(chatList);
        chatScrollPane.setPreferredSize(new Dimension(150, 300));  // Adjust as per your needs

        chatInputField = new JTextField(15);  // Set a desired width for the input field
        sendChatButton = new JButton("Send");

        sendChatButton.addActionListener(e -> {
            String message = chatInputField.getText();
            if (message.length() <= 20) {
                client.sendMessage(message);  // Assuming your Client class has a method to send a message
                chatInputField.setText("");
            } else {
                displayError("Message too long!");  // A method to display error messages
            }
        });

        JPanel chatInputPanel = new JPanel();
        chatInputPanel.setLayout(new BorderLayout());
        chatInputPanel.add(chatInputField, BorderLayout.CENTER);
        chatInputPanel.add(sendChatButton, BorderLayout.EAST);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        frame.add(chatPanel, BorderLayout.EAST);


        // Quit button
        quitButton = new JButton("Quit");
        quitButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(frame, "Are you sure you want to quit?", "Confirm Quit", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                client.quit();
                System.exit(0);
            }
        });
        frame.add(quitButton, BorderLayout.WEST);

        frame.setVisible(true);
    }

    public void closeWaitingScreen() {
        if (preGameFrame != null) {
            preGameFrame.dispose();
            preGameFrame = null;
        }
    }
    public void updateChat(String message) {
        if (chatListModel.size() >= 10) {
            chatListModel.remove(0);
        }
        chatListModel.addElement(message);
        chatList.ensureIndexIsVisible(chatListModel.size() - 1);  // Make sure the most recent message is visible
    }


    private void createWaitingForOpponentFrame() {
        waitingFrame = new JFrame("Waiting...");
        waitingFrame.setLayout(new FlowLayout());
        waitingLabel = new JLabel("Waiting for your opponent...");
        waitingFrame.add(waitingLabel);
        waitingFrame.pack();
        waitingFrame.setVisible(true);
    }

    public void showWaitingScreen() {
        createWaitingForOpponentFrame();
    }

    public void hideWaitingScreen() {
        if (waitingFrame != null) {
            waitingFrame.dispose();
        }
    }

    public void updateBoard(char[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j].setText(Character.toString(board[i][j]));
            }
        }
    }
    public void notifyYourTurn() {
        // 这里可以显示一个提示或更新UI，告诉玩家现在是他的回合。
        JOptionPane.showMessageDialog(frame, "It's your turn!", "Your Turn", JOptionPane.INFORMATION_MESSAGE);
    }
}
