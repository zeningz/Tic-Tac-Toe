import share.ClientInterface;
import share.ServerInterface;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {
    private List<ClientInterface> connectedClients;
    private List<GameSession> activeSessions;
    private Player waitingPlayer = null;
    private Map<ClientInterface, Timer> disconnectionTimers = new HashMap<>();
    private Set<String> playerNames = new HashSet<>();
    private List<Player> allPlayers = new ArrayList<>();
    private Set<String> disconnectPlayers = new HashSet<>();

    private Map<String, GameSession> playerToSessionMap = new HashMap<>();

    private Map<ClientInterface, Timer> heartbeats = new HashMap<>();
    public TicTacToeServer() throws RemoteException {
        connectedClients = new ArrayList<>();
        activeSessions = new ArrayList<>();
    }

    @Override
    public synchronized boolean setPlayer(ClientInterface client, String playerName, boolean newplayer) throws RemoteException {
        Timer previousTimer = disconnectionTimers.remove(client);
        if (previousTimer != null) {
            previousTimer.cancel();
        }


        System.out.println("dc"+disconnectPlayers);
        if (disconnectPlayers.contains(playerName)) {
            handleReconnection(client, playerName);
            return false;  // Player successfully reconnected
        }
        if (newplayer && playerNames.contains(playerName)) {

            throw new RemoteException("Name already in use across the server!");
        }

        Player retrievedPlayer = null;
        for (Player p : allPlayers) {
            if (p.getPlayerName().equals(playerName)) {
                retrievedPlayer = p;
                break;
            }
        }
        System.out.println(waitingPlayer);
        if (waitingPlayer == null) {
            if (retrievedPlayer != null) {
                waitingPlayer = retrievedPlayer;
                waitingPlayer.setClientInterface(client); // Update the client interface for old player
                waitingPlayer.setSymbol('X'); // Update the symbol for old player
            } else {
                waitingPlayer = new Player(playerName, 'X', client);
                allPlayers.add(waitingPlayer);
                playerNames.add(playerName);
            }
            return true;
        } else {
            Random random = new Random();
            char playerOneSymbol = random.nextBoolean() ? 'X' : 'O';
            char playerTwoSymbol = playerOneSymbol == 'X' ? 'O' : 'X';

            waitingPlayer.setSymbol(playerOneSymbol);
            Player newPlayer;
            if (retrievedPlayer != null) {
                newPlayer = retrievedPlayer;
                newPlayer.setClientInterface(client); // Update the client interface for old player
                newPlayer.setSymbol(playerTwoSymbol);
                // Update the symbol for old player
            } else {
                newPlayer = new Player(playerName, playerTwoSymbol, client);
                allPlayers.add(newPlayer);
                playerNames.add(playerName);
            }
            System.out.println("Player 1: " + waitingPlayer.getPlayerName() + " using symbol: " + waitingPlayer.getSymbol());
            System.out.println("Player 2: " + newPlayer.getPlayerName() + " using symbol: " + newPlayer.getSymbol());

            waitingPlayer.getClientInterface().setPlayerDetails(waitingPlayer.getPlayerName(), waitingPlayer.getSymbol());

            newPlayer.getClientInterface().setPlayerDetails(newPlayer.getPlayerName(), newPlayer.getSymbol());

            GameSession newSession = new GameSession(waitingPlayer, newPlayer,this);

            activeSessions.add(newSession);

            playerToSessionMap.put(waitingPlayer.getPlayerName(), newSession);

            playerToSessionMap.put(newPlayer.getPlayerName(), newSession);

            waitingPlayer.getClientInterface().startGame();

            newPlayer.getClientInterface().startGame();

            Player firstPlayerToMove = newSession.getCurrentPlayer();// 获取当前玩家（第一个行动的玩家）的信息
            int rank = getRankOfPlayer(firstPlayerToMove);

            waitingPlayer.getClientInterface().updateCurrentPlayerInfo(firstPlayerToMove.getPlayerName(), firstPlayerToMove.getSymbol(), rank);
            newPlayer.getClientInterface().updateCurrentPlayerInfo(firstPlayerToMove.getPlayerName(), firstPlayerToMove.getSymbol(), rank);
            newSession.startGame();


            waitingPlayer = null;
            return true;  // 玩家已加入新的游戏会话
        }
    }
    private boolean isPlayerInAllPlayers(String playerName) {
        for (Player p : allPlayers) {
            if (p.getPlayerName().equals(playerName)) {
                return true;
            }
        }
        return false;
    }
    private void handleReconnection(ClientInterface client, String playerName) throws RemoteException {
        disconnectPlayers.remove(playerName);
        GameSession sessionToRejoin = playerToSessionMap.get(playerName);
        if (sessionToRejoin != null) {
            // 将客户端重新加入到GameSession
            System.out.println(playerName+"recon");
            sessionToRejoin.reconnectPlayer(client, playerName);

            // 你可能还需要其他逻辑，例如通知其他玩家，或者恢复客户端的游戏状态等
        } else {
            setPlayer(client, playerName, false);
            // 这里处理当无法找到对应的GameSession的情况
        }

    }
    private Player findPlayerByClientInterface(ClientInterface client) {
        for (Player p : allPlayers) {
            if (p.getClientInterface().equals(client)) {
                return p;
            }
        }
        return null;
    }

    public void handleClientDisconnection(ClientInterface client) {
        Player disconnectingPlayer = findPlayerByClientInterface(client);
        if (disconnectingPlayer != null) {
            disconnectPlayers.add(disconnectingPlayer.getPlayerName());
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    disconnectPlayers.remove(disconnectingPlayer.getPlayerName());
                }
            }, 30000);  // 30 seconds
            disconnectionTimers.put(client, timer);
        }
    }


    public void gameFinished(GameSession session) throws RemoteException {
        // 你的具体实现，例如：
        // Remove the session from the list of active sessions
        activeSessions.remove(session);

    }

    public int getRankOfPlayer(Player player) {
        System.out.println(allPlayers);
        System.out.println(playerNames);
        List<Player> sortedPlayers = new ArrayList<>(allPlayers);

        sortedPlayers.sort((p1, p2) -> {
            if (p1.getPoints() == p2.getPoints()) {
                return p1.getPlayerName().compareTo(p2.getPlayerName());
            }
            return Integer.compare(p2.getPoints(), p1.getPoints());
        });

        return sortedPlayers.indexOf(player) + 1;
    }



    public void removeGameSession(GameSession session) {
        activeSessions.remove(session);
    }

    @Override
    public void quit(ClientInterface client) throws RemoteException {
        // Find the player associated with the given client
        Player quittingPlayer = null;
        for (Player p : allPlayers) {
            if (p.getClientInterface().equals(client)) {
                quittingPlayer = p;
                break;
            }
        }

        // Find the GameSession associated with the quitting player
        GameSession quittingSession = null;
        for (GameSession session : activeSessions) {
            if (session.hasPlayer(client)) {
                quittingSession = session;
                break;
            }
        }

        if (quittingSession != null) {
            // Find the other player in the session
            Player otherPlayer = (quittingPlayer == quittingSession.getPlayer1()) ? quittingSession.getPlayer2() : quittingSession.getPlayer1();

            // Notify the other player that they have won
            otherPlayer.getClientInterface().gameEnded(quittingPlayer.getPlayerName() + " has quit! You win!");

            // Remove the session from activeSessions
            activeSessions.remove(quittingSession);
        }

        // Remove the player from the lists
        if (quittingPlayer != null) {
            allPlayers.remove(quittingPlayer);
            playerNames.remove(quittingPlayer.getPlayerName());
        }
    }



    @Override
    public void makeMove(ClientInterface client, int row, int col) throws RemoteException {
        GameSession session = findSessionByPlayer(client);
        Player currentplayer = session.getCurrentPlayer();
        char symbol = currentplayer.getSymbol();
        try{
            session.getPlayer1().getClientInterface().heartbeat();
        }catch (RemoteException e){
            handleClientDisconnection(session.getPlayer1().getClientInterface());
        }
        try{
            session.getPlayer2().getClientInterface().heartbeat();
        }catch (RemoteException e){
            handleClientDisconnection(session.getPlayer2().getClientInterface());
        }

        if (session != null) {
            Player player = findPlayerByClient(client, session);
            Player nextPlayer = session.makeMove(player, row, col);  // 获取返回的玩家
            int rank = getRankOfPlayer(nextPlayer);

            // Update info for both players
            Player otherPlayer = (nextPlayer == session.getPlayer1()) ? session.getPlayer2() : session.getPlayer1();
            nextPlayer.getClientInterface().updateCurrentPlayerInfo(nextPlayer.getPlayerName(), nextPlayer.getSymbol(), rank);
            otherPlayer.getClientInterface().updateCurrentPlayerInfo(nextPlayer.getPlayerName(), nextPlayer.getSymbol(), rank);
        }

    }



    private GameSession findSessionByPlayer(ClientInterface client) {
        for (GameSession session : activeSessions) {
            if (session.hasPlayer(client)) {  // 假设你的GameSession类有一个方法可以根据ClientInterface检查玩家是否存在
                return session;
            }
        }
        return null;
    }
    private Player findPlayerByClient(ClientInterface client, GameSession session) throws RemoteException {
        if (session.getPlayer1().getClientInterface().equals(client)) {
            return session.getPlayer1();
        } else if (session.getPlayer2().getClientInterface().equals(client)) {
            return session.getPlayer2();
        }
        return null;
    }


    @Override
    public char[][] getBoard(ClientInterface client) {
        // TODO: Implement board retrieval.
        return null;
    }
    @Override
    public void sendMessage(ClientInterface sender, String message) throws RemoteException {
        GameSession game = findSessionByPlayer(sender);
        Player currentplayer = game.getCurrentPlayer();
        char symbol = currentplayer.getSymbol();
        try{
            game.getPlayer1().getClientInterface().heartbeat();
        }catch (RemoteException e){
            handleClientDisconnection(game.getPlayer1().getClientInterface());
        }
        try{
            game.getPlayer2().getClientInterface().heartbeat();
        }catch (RemoteException e){
            handleClientDisconnection(game.getPlayer2().getClientInterface());
        }
        for (GameSession session : activeSessions) {
            if (session.hasPlayer(sender)) {
                // 发送消息给这个GameSession中的另一个玩家
                if (session.getPlayer1().getClientInterface().equals(sender)) {
                    session.getPlayer2().getClientInterface().receiveMessage(message);
                } else {
                    session.getPlayer1().getClientInterface().receiveMessage(message);
                }
                break;
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
