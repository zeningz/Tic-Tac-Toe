import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Point;
import java.rmi.RemoteException;
import java.util.List;
import java.util.ArrayList;

import java.util.Random;

public class ClientGUI {
    private Client client;
    private JFrame frame;
    private JButton[][] boardButtons;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private JButton quitButton;
    private JLabel statusLabel;
    private JLabel timerLabel;

    private JLabel playerInfoLabel;
    private Timer timer;
    private JFrame preGameFrame;
    private JLabel gameStatusLabel;
    private JTextField playerNameInput;
    private JButton connectButton;
//    private JLabel errorMessageLabel;
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

    private String playerName;
    private char playerSymbol;
    public ClientGUI(Client client) {
        this.client = client;
        createPreGameFrame();
    }

    private void createPreGameFrame() {
        preGameFrame = new JFrame("Enter Player Name");
        preGameFrame.setLayout(new FlowLayout());

        playerNameInput = new JTextField(20);
        connectButton = new JButton("Connect");
//        errorMessageLabel = new JLabel("");
        statusLabel = new JLabel("");  // Move this line up before adding to preGameFrame

        preGameFrame.add(playerNameInput);
        preGameFrame.add(connectButton);
//        preGameFrame.add(errorMessageLabel);
        preGameFrame.add(statusLabel);
        preGameFrame.pack();
        preGameFrame.setVisible(true);
        preGameFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);


        preGameFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                int result = JOptionPane.showConfirmDialog(preGameFrame, "Are you sure you want to quit?", "Confirm Quit", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.quit();
                    System.exit(0);
                }
            }
        });

        connectButton.addActionListener(e -> {
            String playerName = playerNameInput.getText();
            if (!playerName.isEmpty()) {
                String connectionResult = null;
                try {
                    connectionResult = client.connectToServer(playerName);
                } catch (RemoteException ex) {
                    System.err.println("Fail to Connect from client to server");
                }
                if ("waiting".equals(connectionResult)) {

                } else if ("reconncet".equals(connectionResult)){

                } else if ("play".equals(connectionResult)){

                } else if ("NAME_IN_USE".equals(connectionResult)) {
                    System.out.println("name");
                    statusLabel.setText("Name already in use!");
                } else if ("SERVER_DISCONNECTED".equals(connectionResult)) {
                    statusLabel.setText("Disconnected from server!");
                }
                preGameFrame.pack();
                preGameFrame.repaint();
            }

        });

    }

    public void displayError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    public void updateTimer(int seconds) {
        timerLabel.setText("Time: " + seconds);
        timerLabel.repaint(); // 确保标签得到重新绘制
    }

    public void updatePlayerTurn(String playerName) {
        turnLabel.setText("Player '" + playerName + "'s Turn");
    }


    public void createGameFrame() {
        if (frame != null) {
            return;
        }


        frame = new JFrame("Tic Tac Toe");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // Timer label setup
        timerLabel = new JLabel("Time: 20");
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timerPanel.add(timerLabel);

        // Top panel setup
        topPanel = new JPanel(new BorderLayout());
        gameStatusLabel = new JLabel("Finding Player...", SwingConstants.CENTER);
        rankLabel = new JLabel("Rank #-- 'username' (X/O)", SwingConstants.CENTER);
        turnLabel = new JLabel("Player 'username's Turn", SwingConstants.CENTER);

        topPanel.add(gameStatusLabel, BorderLayout.NORTH);
        topPanel.add(rankLabel, BorderLayout.CENTER);
        topPanel.add(turnLabel, BorderLayout.SOUTH);

        // North panel setup
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(timerPanel, BorderLayout.NORTH);
        northPanel.add(topPanel, BorderLayout.CENTER);

        frame.add(northPanel, BorderLayout.NORTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(frame, "Are you sure you want to quit?", "Confirm Quit", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.quit();
                    System.exit(0);
                }
            }
        });

        // Tic Tac Toe board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3));
        boardButtons = new JButton[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j] = new JButton("");
                boardButtons[i][j].setEnabled(false);
                boardButtons[i][j].setBackground(Color.CYAN);
                boardButtons[i][j].setOpaque(true);
                boardButtons[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));

                final int x = i;
                final int y = j;
                boardButtons[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Button clicked!");

                        JButton clickedButton = (JButton) e.getSource();
                        System.out.println("Button text: " + clickedButton.getText());

                        if (clickedButton.getText().equals("")) { // 确保该格子还没有被下过棋
                            System.out.println("Button clicked at position: " + x + "," + y);
                            client.makeMove(x, y); // 告诉Client类的实例玩家在(x, y)位置下了一步棋
                        }
                    }
                });
                boardPanel.add(boardButtons[i][j]);
            }
        }
        frame.add(boardPanel, BorderLayout.CENTER);




        // Chat window
        chatListModel = new DefaultListModel<>();
        chatList = new JList<>(chatListModel);
        chatScrollPane = new JScrollPane(chatList);
        chatScrollPane.setPreferredSize(new Dimension(150, 300));  // Adjust as per your needs

        chatInputField = new JTextField(15);  // Set a desired width for the input field
        sendChatButton = new JButton("Send");
        sendChatButton.setEnabled(false);
        sendChatButton.addActionListener(e -> {
            String message = chatInputField.getText().trim();  // 使用 trim() 移除开头和结尾的空白
            if (message.isEmpty()) {
                displayError("Message cannot be empty!");
                return;
            }
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
    public void updateTimeLeft() {
        // 更新timeLabel的文本以显示剩余的时间
        timerLabel.setText("Time: 20");
    }
    public List<Point> getAvailableMoves() {
        List<Point> availableMoves = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                // 假设一个未被点击的按钮的文本是空的
                if (boardButtons[i][j].getText().equals("")) {
                    availableMoves.add(new Point(i, j));
                }
            }
        }

        return availableMoves;
    }

    public void displayNotification(String message) {
        JOptionPane.showMessageDialog(frame, message, "Game Result", JOptionPane.INFORMATION_MESSAGE);
    }
    public void gameEndedPrompt(String winnerMessage) {
        SwingUtilities.invokeLater(() -> {
            String message = winnerMessage + "\nDo you want to play again?";
            int result = JOptionPane.showConfirmDialog(frame, message, "Game Over", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                // Reconnect to server for a new game or show pre-game frame
                startFindingPlayer();
            } else {
                // Disconnect and exit
                client.quit();
                System.exit(0);
            }
        });
    }
    public void startFindingPlayer() {
        if (frame != null) {
            frame.dispose();  // Close the game frame
            frame = null;
        }
        createGameFrame();  // Create a new game frame
        gameStatusLabel.setText("Finding Player...");  // Update the status
        client.startFindingPlayer();  // Notify the server to start finding another player
    }

    public void updatePlayerInfo(String playerName, char playerSymbol, int rank) {
        SwingUtilities.invokeLater(() -> {
            rankLabel.setText("Rank #" + rank + " '" + playerName + "' (" + playerSymbol + ")");
            turnLabel.setText("Player '" + playerName + "'s Turn");
            frame.repaint();
        });
    }


    public void updateChat(String message) {
        if (chatListModel.size() >= 10) {
            chatListModel.remove(0);
        }
        chatListModel.addElement(message);
        chatList.ensureIndexIsVisible(chatListModel.size() - 1);  // Make sure the most recent message is visible
    }
    public void showWaitingScreen() {
        preGameFrame.dispose(); // 关闭 preGameFrame
        createGameFrame(); // 创建游戏界面
        gameStatusLabel.setText("Finding Player...");
    }
    public void startGame() {
        preGameFrame.dispose(); // 关闭 preGameFrame
        createGameFrame();
        gameStatusLabel.setText("Game On!");  // 或其他适当的消息
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j].setEnabled(true);
            }
        }
        sendChatButton.setEnabled(true);
    }

    public void freezeGameUI() {
        // 禁用 boardPanel 和其他相关的 GUI 组件
        frame.setEnabled(false);
        // 你还可以显示一个消息，告诉玩家游戏已被暂停。
        JOptionPane.showMessageDialog(null, "Game is paused due to opponent's disconnection.");
    }
    public void updateBoard(char[][] board) {
        if (boardButtons == null) {
            System.out.println("boardButtons is not initialized!");
            return;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                char boardChar = board[i][j];
                String buttonChar = boardButtons[i][j].getText();

                if (boardChar == ' ' && buttonChar.equals("")) {
                    continue;
                }
                if (boardChar != ' ' && !buttonChar.equals(Character.toString(boardChar))) {
                    boardButtons[i][j].setText(Character.toString(boardChar));
                }
            }
        }
    }


    public void setPlayerInfo(String playerName, char playerSymbol) {
        this.playerName = playerName;
        this.playerSymbol = playerSymbol;
    }



}
