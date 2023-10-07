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
            System.out.println("Connect button clicked!");
            String playerName = playerNameInput.getText();
            if (!playerName.isEmpty()) {
                try {
                    boolean success = client.connectToServer(playerName);
                    if (success) {
                        preGameFrame.dispose();
                        createGameFrame();
                    } else {
                        errorMessageLabel.setText("Name already in use!");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    errorMessageLabel.setText("Error connecting to server!");
                }
            }
        });

    }
    public void displayError(String message) {
        // 显示一个错误对话框或更新GUI以显示错误消息
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void createGameFrame() {
        System.out.println("Creating game frame...");
        frame = new JFrame("Tic Tac Toe");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // ... [Rest of the game GUI initialization, same as your previous code]

        frame.setVisible(true);
        System.out.println("Game frame should now be visible.");
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
