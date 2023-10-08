import java.rmi.Naming;
import java.rmi.RemoteException;
import share.ClientInterface;
import share.ServerInterface;
import java.rmi.server.UnicastRemoteObject;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

public class Client implements ClientInterface {
    private ClientInterface clientStub;
    private ServerInterface server;
    private ClientGUI gui;

    public Client(String serverURL) {
        try {
            clientStub = (ClientInterface) UnicastRemoteObject.exportObject(this, 0);
            server = (ServerInterface) Naming.lookup(serverURL);
            gui = new ClientGUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void waitForOpponent() {
        SwingUtilities.invokeLater(() -> {
            // 这里你可以更新你的 GUI，例如显示一个 JOptionPane 对话框或者更新某个标签，告诉玩家他们正在等待对手。
            JOptionPane.showMessageDialog(null, "Waiting for your opponent...");
        });
    }

    @Override
    public void startGame() {
        SwingUtilities.invokeLater(() -> {
            // 这里你可以更新你的 GUI，例如关闭上面的对话框（如果你使用了它）并显示游戏的主界面。
            // 如果你已经有了一个方法来显示游戏的主界面，只需在这里调用它。
            gui.createGameFrame();
        });
    }
    public boolean connectToServer(String playerName) {
        System.out.println("Attempting to connect with name: " + playerName);
        try {
            server.setPlayer(clientStub, playerName);
            return true;
        } catch (RemoteException e) {
            if (e.getMessage().contains("Name alrsseady in use")) {  // 修改这里，使用contains而不是equals
                gui.displayError("Name already in use. Please choose a different name.");
                return false;
            } else {
                e.printStackTrace();
                return false;
            }
        }
    }


    public void makeMove(int row, int col) {
        try {
            server.makeMove(clientStub, row, col);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void quit() {
        try {
            server.quit(clientStub);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public void sendMessage(String message) {
        try {
            System.out.println("Attempting to send message: " + message);
            server.sendMessage(clientStub, message);
        } catch (RemoteException e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void yourTurn() {
        gui.notifyYourTurn();
    }
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
        gui.updateChat(message);
    }


    public void updateGame(char[][] board) {
        gui.updateBoard(board);
    }

    public static void main(String[] args) {
        new Client("rmi://localhost/TicTacToe");
    }
}
