import share.ServerInterface;
import share.ClientInterface;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {
    private List<ClientInterface> connectedClients;
    private List<GameSession> activeSessions;
    private Player waitingPlayer = null;
    public TicTacToeServer() throws RemoteException {
        connectedClients = new ArrayList<>();
        activeSessions = new ArrayList<>();
    }

    @Override
    public boolean setPlayer(ClientInterface client, String playerName) throws RemoteException {
        for (GameSession session : activeSessions) {
            if (session.hasPlayer(playerName)) {
                throw new RemoteException("Name already in use!");
            }
        }

        if (waitingPlayer == null) {
            waitingPlayer = new Player(playerName, 'X', client);  // 默认符号是 'X'
            client.waitForOpponent();  // 通知玩家等待对手
            return true;  // 玩家已加入等待队列
        } else {
            Random random = new Random();
            char playerOneSymbol = random.nextBoolean() ? 'X' : 'O';
            char playerTwoSymbol = playerOneSymbol == 'X' ? 'O' : 'X';

            waitingPlayer.setSymbol(playerOneSymbol);
            Player newPlayer = new Player(playerName, playerTwoSymbol, client);

            System.out.println("Player 1: " + waitingPlayer.getPlayerName() + " using symbol: " + waitingPlayer.getSymbol());
            System.out.println("Player 2: " + newPlayer.getPlayerName() + " using symbol: " + newPlayer.getSymbol());

            GameSession newSession = new GameSession(waitingPlayer, newPlayer);
            activeSessions.add(newSession);


            waitingPlayer = null;
            return true;  // 玩家已加入新的游戏会话
        }
    }






    public void removeGameSession(GameSession session) {
        activeSessions.remove(session);
    }

    @Override
    public void quit(ClientInterface client) {
        // TODO: Implement player disconnection logic.
    }

    @Override
    public void makeMove(ClientInterface client, int row, int col) {
        // TODO: Implement move logic.
    }

    @Override
    public char[][] getBoard(ClientInterface client) {
        // TODO: Implement board retrieval.
        return null;
    }
    @Override
    public void sendMessage(ClientInterface sender, String message) throws RemoteException {
        // 这里只是一个简单的实现。实际上，你可能想要将消息只发送给特定的GameSession的玩家。
        for (ClientInterface client : connectedClients) {
            if (!client.equals(sender)) { // 不给发送者发消息
                client.receiveMessage(message);
            }
        }
    }

    public void start() {
        // TODO: RMI registry initialization and other server setup.
    }
    public static void main(String[] args) {
        try {
            TicTacToeServer server = new TicTacToeServer();
            server.start();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
