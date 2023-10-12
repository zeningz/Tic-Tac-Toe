import java.awt.*;
import java.rmi.Naming;
import java.rmi.RemoteException;

import share.ClientInterface;
import share.ServerInterface;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import java.util.List;


public class Client implements ClientInterface {
    private Timer moveTimer;
    private int countdown = 20; // 初始时间设置为20秒

    private ClientInterface clientStub;

    private ServerInterface server;
    private ClientGUI gui;
    private String playerName;
    private char playerSymbol;
    private Timer heartbeatTimer;
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
            gui.startGame();
        });
    }
    public String connectToServer(String playerName) {
        System.out.println("Attempting to connect with name: " + playerName);
        try {
            if(server.setPlayer(clientStub, playerName,true)) {
                System.out.println("ok");
                return "SUCCESS";
            }
            else{
                return "RECONNECT";
            }
        } catch (RemoteException e) {
            if (e.getMessage().contains("Name already in use across the server!")) {  // 使用contains而不是equals
                return "NAME_IN_USE";
            } else {
                e.printStackTrace();
                return "SERVER_DISCONNECTED";
            }
        }
    }
    @Override
    public void heartbeat(){}
    private void startTimer() {
        countdown = 20; // 重置倒计时
        if (moveTimer != null) {
            moveTimer.stop();
        }
        moveTimer = new Timer(1000, e -> {
            countdown--;
            gui.updateTimer(countdown); // 更新GUI上的计时器显示
            if (countdown <= 0) {
                makeRandomMove(); // 倒计时结束，随机进行下一步
                moveTimer.stop(); // 停止计时器
            }
        });
        moveTimer.start();
    }
    private void makeRandomMove() {
        List<Point> availableMoves = gui.getAvailableMoves();
        if (availableMoves.isEmpty()) {
            return;
        }
        Random rand = new Random();
        Point randomMove = availableMoves.get(rand.nextInt(availableMoves.size()));
        makeMove(randomMove.x, randomMove.y);
    }

    @Override
    public void updateCurrentPlayerInfo(String playerName, char symbol, int rank) throws RemoteException {
        // 在这里，我们会使用从服务器收到的信息来更新 GUI
        gui.updatePlayerInfo(playerName, symbol, rank);
        if (this.playerName.equals(playerName)) {
            startTimer();  // 如果这是当前客户端的玩家，启动计时器
        } else {
            stopTimer();   // 否则停止计时器
        }
    }
    private void stopTimer() {
        if (moveTimer != null) {
            moveTimer.stop();
            gui.updateTimeLeft();
        }
    }

    public void makeMove(int row, int col) {
        stopTimer();
        System.out.println("Entering makeMove");
        try {
            System.out.println("Before calling server.makeMove");
            server.makeMove(clientStub, row, col);
            System.out.println("After calling server.makeMove");

        } catch (RemoteException e) {
            System.out.println("Exception in makeMove");
            e.printStackTrace();
        }
        System.out.println("Exiting makeMove");

    }


    @Override
    public void setPlayerDetails(String playerName, char playerSymbol) throws RemoteException {
        this.playerName = playerName;
        this.playerSymbol = playerSymbol;
        gui.setPlayerInfo(playerName, playerSymbol);
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
            String formattedMessage = this.playerName + ": " + message;
            server.sendMessage(clientStub, formattedMessage);

            // 将自己的消息添加到自己的聊天框中
            receiveMessage(formattedMessage);
        } catch (RemoteException e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void startFindingPlayer() {
        try {
            server.setPlayer(this.clientStub, this.playerName,false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }



    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
        gui.updateChat(message);
    }


    public void updateGame(char[][] board) {
        gui.updateBoard(board);
    }
    @Override
    public void createGameFrame() {

        gui.createGameFrame();
    }
    public void gameEnded(String winnerMessage) {
        // Notify the GUI to show a dialog to the user about game ending and ask for the next step
        gui.gameEndedPrompt(winnerMessage);
    }

    @Override
    public void displayNotification(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> gui.displayNotification(message));
    }
    public static void main(String[] args) {
        new Client("rmi://localhost/TicTacToe");
    }
}
