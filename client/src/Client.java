import java.awt.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import share.ClientInterface;
import share.ServerInterface;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import java.util.List;
import java.net.MalformedURLException;
/**
 * This is the main client class responsible for handling the client side of the game.
 * It implements the ClientInterface which provides methods that the server can call remotely.
 */

public class Client implements ClientInterface {
    // Countdown timer for client's move
    private Timer moveTimer;
    private int countdown = 20;

    private ClientInterface clientStub;

    private ServerInterface server;
    private ClientGUI gui;
    private String playerName;
    private char playerSymbol;
    private Timer heartbeatCheckTimer;
    /**
     * Constructor for the Client class.
     * @param serverURL - The RMI URL for the server.
     * @param username - The username of the player.
     */
    public Client(String serverURL, String username) {
        // Export the client object so that the server can call its methods remotely
        try {
            clientStub = (ClientInterface) UnicastRemoteObject.exportObject(this, 0);
        } catch (RemoteException e) {
            System.err.println("Error while exporting client object");
            return;
        }

        try {
            // Connect to the server
            server = (ServerInterface) Naming.lookup(serverURL);
        } catch (MalformedURLException e) {
            System.err.println("The provided server URL is malformed. Please check your input.");
            return;
        } catch (NotBoundException e) {
            System.err.println("Server not found at the specified URL.");
            return;
        } catch (RemoteException e) {
            System.err.println("Remote exception while connecting to the server.");
            return;
        }

        // Initialize the GUI
        gui = new ClientGUI(this, username);
        // Start checking for heartbeat from the server
        startHeartbeatCheck();
    }
    /**
     * This method starts a Timer that checks for a heartbeat signal from the server.
     * If the server does not respond, it's assumed to be disconnected.
     */
    private void startHeartbeatCheck() {
        heartbeatCheckTimer = new Timer(5000, e -> {
            try {
                server.heartbeat();
            } catch (RemoteException ex) {
                try {
                    displayNotification("Server is disconnected. The client will close in 5 seconds.");
                } catch (RemoteException exc) {
                    System.err.println("Fail to display Notification");
                }
                new Timer(5000, evt -> System.exit(0)).start();
            }
        });
        heartbeatCheckTimer.start();
    }

    @Override
    public void waitForOpponent() {
        SwingUtilities.invokeLater(() -> {

            JOptionPane.showMessageDialog(null, "Waiting for your opponent...");
        });
    }
    @Override
    public void freezeGameUI() {
        gui.freezeGameUI();
    }

    @Override
    public void showWaitingScreen() {
        SwingUtilities.invokeLater(() -> {

            gui.showWaitingScreen();
        });
    }
    /**
     * Starts a countdown timer for the player's move.
     * If the timer reaches 0, a random move is made on behalf of the player.
     */
    @Override
    public void startGame() {
        SwingUtilities.invokeLater(() -> {

            gui.startGame();
        });
    }
    @Override
    public String connectToServer(String playerName) throws RemoteException {
        System.out.println("Attempting to connect with name: " + playerName);
        try {
            String result =server.setPlayer(clientStub, playerName,true);
            return result;
        } catch (RemoteException e) {
            if (e.getMessage().contains("Name already in use across the server!")) {
                return "NAME_IN_USE";
            } else {

                return "SERVER_DISCONNECTED";
            }
        }
    }
    @Override
    public void heartbeat(){}
    private void startTimer() {
        countdown = 20;
        if (moveTimer != null) {
            moveTimer.stop();
        }
        moveTimer = new Timer(1000, e -> {
            countdown--;
            gui.updateTimer(countdown);
            if (countdown <= 0) {
                makeRandomMove();
                moveTimer.stop();
            }
        });
        moveTimer.start();
    }
    /**
     * Makes a random move on the board.
     */
    private void makeRandomMove() {
        List<Point> availableMoves = gui.getAvailableMoves();
        if (availableMoves.isEmpty()) {
            return;
        }
        Random rand = new Random();
        Point randomMove = availableMoves.get(rand.nextInt(availableMoves.size()));
        makeMove(randomMove.x, randomMove.y);
        try {
            displayNotification("You took too long. A random move has been made for you.");
        }catch (RemoteException e){
            System.err.println("Failed to send notification to client");
        }
    }

    @Override
    public void updateCurrentPlayerInfo(String playerName, char symbol, int rank) throws RemoteException {

        SwingUtilities.invokeLater(() -> {

            gui.updatePlayerInfo(playerName, symbol, rank);
        });
        if (this.playerName.equals(playerName)) {
            startTimer();
        } else {
            stopTimer();
        }

    }
    private void stopTimer() {
        if (moveTimer != null) {
            moveTimer.stop();
            gui.updateTimeLeft();
        }
    }
    /**
     * Makes a move on the board.
     * @param row - The row where the move is to be made.
     * @param col - The column where the move is to be made.
     */
    public void makeMove(int row, int col) {
        stopTimer();

        try {

            server.makeMove(clientStub, row, col);


        } catch (RemoteException e) {

            System.err.println("Fail to makeMove");
        }


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
            System.err.println("Fail to quit");
        }
    }
    public void sendMessage(String message) {
        try {
            int rank = server.getRankOfClient(clientStub);
            String formattedMessage = this.playerName + " (rank: "+ rank + " ): " + message;
            server.sendMessage(clientStub, formattedMessage);

            receiveMessage(formattedMessage);
        } catch (RemoteException e) {
            System.err.println("Error sending message");
        }
    }
    public void startFindingPlayer() {
        try {
            server.setPlayer(this.clientStub, this.playerName,false);
        } catch (RemoteException e) {
            System.err.println("Fail to find opponent");
        }
    }



    public void receiveMessage(String message) {
        gui.updateChat(message);
    }


    public void updateGame(char[][] board) {

        SwingUtilities.invokeLater(() -> {

            gui.updateBoard(board);
        });
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
    /**
     * Main method to start the client application.
     * @param args - Command line arguments.
     */
    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: java Client <RMI URL>");
            System.exit(1);
        }
        String username = args[0];
        String serverIP = args[1];
        int serverPort = Integer.parseInt(args[2]);
        String rmiURL = "rmi://" + serverIP + ":" + serverPort + "/TicTacToe";
        System.out.println(rmiURL);
        new Client(rmiURL,username);
//        new Client("rmi://localhost/TicTacToe");
    }
}
