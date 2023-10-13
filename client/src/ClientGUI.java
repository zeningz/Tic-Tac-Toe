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
/**
 * This class represents the GUI for the Tic Tac Toe client.
 * It provides the interface through which the user interacts with the game.
 */
public class ClientGUI {
    // Instance variables representing the client, frame, game board and other components.
    private Client client;
    private JFrame frame;
    private JButton[][] boardButtons;
    private JButton quitButton;
    private JLabel statusLabel;
    private JLabel timerLabel;

    private JFrame preGameFrame;
    private JLabel gameStatusLabel;
    private JTextField playerNameInput;
    private JButton connectButton;

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
    /**
     * Constructor for the ClientGUI class.
     * @param client - The client instance.
     * @param username - The name of the user.
     */
    public ClientGUI(Client client, String username) {
        this.client = client;
        createPreGameFrame(username);
    }
    /**
     * This method creates the initial frame where users input their name and connect to the game.
     */
    private void createPreGameFrame(String username) {
        preGameFrame = new JFrame("Enter Player Name");
        preGameFrame.setLayout(new FlowLayout());
        playerNameInput = new JTextField(username, 20); // 设置文本框的初始文本为 username
        playerNameInput.setEditable(false);
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

                    statusLabel.setText("Name already in use!");
                } else if ("SERVER_DISCONNECTED".equals(connectionResult)) {
                    statusLabel.setText("Disconnected from server!");
                }
                preGameFrame.pack();
                preGameFrame.repaint();
            }

        });

    }
    /**
     * Display error message dialogs.
     */
    public void displayError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    /**
     * Update the displayed timer with the remaining seconds for the player's move.
     */
    public void updateTimer(int seconds) {
        timerLabel.setText("Time: " + seconds);
        timerLabel.repaint();
    }
    /**
     * Update the label to indicate whose turn it is.
     */
    public void updatePlayerTurn(String playerName) {
        turnLabel.setText("Player '" + playerName + "'s Turn");
    }

    /**
     * Create the main game frame with the Tic Tac Toe board, chat, and other components.
     */
    public void createGameFrame() {
        if (frame != null) {
            return;
        }


        frame = new JFrame("Tic Tac Toe " + playerName);
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


                        JButton clickedButton = (JButton) e.getSource();


                        if (clickedButton.getText().equals("")) {

                            client.makeMove(x, y);
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
    /**
     * Reset the timer display.
     */
    public void updateTimeLeft() {

        timerLabel.setText("Time: 20");
    }
    /**
     * Get the list of available moves (empty spots) on the board.
     */
    public List<Point> getAvailableMoves() {
        List<Point> availableMoves = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {

                if (boardButtons[i][j].getText().equals("")) {
                    availableMoves.add(new Point(i, j));
                }
            }
        }

        return availableMoves;
    }
    /**
     * Display a notification dialog with the specified message.
     */
    public void displayNotification(String message) {
        JOptionPane.showMessageDialog(frame, message, "Game Result", JOptionPane.INFORMATION_MESSAGE);
    }
    /**
     * Prompt the player when a game ends, asking if they want to play again.
     */
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
    /**
     * Begin the process of finding another player for a new game.
     */
    public void startFindingPlayer() {
        if (frame != null) {
            frame.dispose();  // Close the game frame
            frame = null;
        }
        createGameFrame();  // Create a new game frame
        gameStatusLabel.setText("Finding Player...");  // Update the status
        client.startFindingPlayer();  // Notify the server to start finding another player
    }
    /**
     * Update player information in the GUI.
     */
    public void updatePlayerInfo(String playerName, char playerSymbol, int rank) {
        SwingUtilities.invokeLater(() -> {
            rankLabel.setText("Rank #" + rank + " '" + playerName + "' (" + playerSymbol + ")");
            turnLabel.setText("Player '" + playerName + "'s Turn");
            frame.repaint();
        });
    }

    /**
     * Append a message to the chat window.
     */
    public void updateChat(String message) {
        if (chatListModel.size() >= 10) {
            chatListModel.remove(0);
        }
        chatListModel.addElement(message);
        chatList.ensureIndexIsVisible(chatListModel.size() - 1);
    }
    /**
     * Display the waiting screen (while searching for another player).
     */
    public void showWaitingScreen() {
        preGameFrame.dispose();
        createGameFrame();
        gameStatusLabel.setText("Finding Player...");
    }
    /**
     * Begin the actual game.
     */
    public void startGame() {
        preGameFrame.dispose();
        createGameFrame();
        gameStatusLabel.setText("Game On!");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardButtons[i][j].setEnabled(true);
            }
        }
        sendChatButton.setEnabled(true);
    }
    /**
     * Freeze the game UI, typically due to a player's disconnection.
     */
    public void freezeGameUI() {
        frame.setEnabled(false);
        JOptionPane.showMessageDialog(null, "Game is paused due to opponent's disconnection.");
    }
    /**
     * Update the state of the game board based on the provided board data.
     */
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
    /**
     * Set the player's name and symbol for this GUI instance.
     */

    public void setPlayerInfo(String playerName, char playerSymbol) {
        this.playerName = playerName;
        this.playerSymbol = playerSymbol;
    }

}
