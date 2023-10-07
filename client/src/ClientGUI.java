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

    public void createGameFrame() {
        waitingFrame.dispose();
        frame = new JFrame("Tic Tac Toe");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // ... [Rest of the game GUI initialization]

        frame.setVisible(true);
    }
    public void closeWaitingScreen() {
        if (preGameFrame != null) {
            preGameFrame.dispose();
            preGameFrame = null;
        }
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

    public void updateChat(String message) {
        chatArea.append(message + "\n");
    }
}
