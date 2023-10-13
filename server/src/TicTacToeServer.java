import share.ClientInterface;
import share.ServerInterface;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
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
import java.rmi.Naming;
import java.net.MalformedURLException;

public class TicTacToeServer extends UnicastRemoteObject implements ServerInterface {
    // Lists to keep track of connected clients and ongoing game sessions
    private List<ClientInterface> connectedClients;
    private List<GameSession> activeSessions;
    private Player waitingPlayer = null;
    private Map<ClientInterface, Timer> disconnectionTimers = new HashMap<>();
    private Set<String> playerNames = new HashSet<>();
    private List<Player> allPlayers = new ArrayList<>();
    private Set<String> disconnectPlayers = new HashSet<>();

    private Map<String, GameSession> playerToSessionMap = new HashMap<>();
    private final long HEARTBEAT_INTERVAL = 5000;
    private Map<ClientInterface, Timer> heartbeats = new HashMap<>();
    // Constructor for the server
    public TicTacToeServer() throws RemoteException {
        connectedClients = new ArrayList<>();
        activeSessions = new ArrayList<>();
        startHeartbeat();
    }
    // Starts a timer to periodically check if clients are still connected.
    public void startHeartbeat() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 创建一个临时列表来存储断开连接的客户端
                List<ClientInterface> disconnectedClients = new ArrayList<>();

                for (Player player : allPlayers) {
                    try {
                        player.getClientInterface().heartbeat();
                    } catch (RemoteException e) {
                        disconnectedClients.add(player.getClientInterface());
                    }
                }

                // 现在，处理所有断开连接的客户端
                for (ClientInterface client : disconnectedClients) {
                    handleClientDisconnection(client);
                }
            }
        }, 0, HEARTBEAT_INTERVAL);
    }

    // Sets the player's client interface and returns the game state for that player (waiting, playing, or reconnecting).
    @Override
    public synchronized String setPlayer(ClientInterface client, String playerName, boolean newplayer) throws RemoteException {

        Timer previousTimer = disconnectionTimers.remove(client);
        if (previousTimer != null) {
            previousTimer.cancel();
        }



        if (disconnectPlayers.contains(playerName)) {
            handleReconnection(client, playerName);
            return "reconnect";  // Player successfully reconnected
        }
        if (newplayer && playerNames.contains(playerName) && !disconnectPlayers.contains(playerName)) {

            throw new RemoteException("Name already in use across the server!");
        }

        Player retrievedPlayer = null;
        for (Player p : allPlayers) {
            if (p.getPlayerName().equals(playerName)) {
                retrievedPlayer = p;
                break;
            }
        }
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
            waitingPlayer.getClientInterface().setPlayerDetails(waitingPlayer.getPlayerName(), waitingPlayer.getSymbol());
            waitingPlayer.getClientInterface().showWaitingScreen();
            return "waiting";
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
            return "play";  // 玩家已加入新的游戏会话
        }
    }
    // Checks if both players in a given session are connected.
    public boolean areBothPlayersConnected(GameSession session) {
        Player player1 = session.getPlayer1();
        Player player2 = session.getPlayer2();

        // 如果两名玩家都不在disconnectPlayers集合中，则都已连接
        return !disconnectPlayers.contains(player1.getPlayerName()) && !disconnectPlayers.contains(player2.getPlayerName());
    }
    // Handles reconnection of a player.
    private void handleReconnection(ClientInterface client, String playerName) throws RemoteException {

        // 1. Remove the player from disconnectPlayers list
        disconnectPlayers.remove(playerName);
        // 2. Cancel the disconnection timer for the player and remove it from the map
        Timer previousTimer = disconnectionTimers.get(client);
        if (previousTimer != null) {
            previousTimer.cancel();
            disconnectionTimers.remove(client);
        }

        // 2. Remove the old ClientInterface from disconnectionTimers map
        disconnectionTimers.remove(client);

        Player reconnectingPlayer = null;

        // 3. Update the ClientInterface of the reconnecting player in allPlayers list
        for (Player player : allPlayers) {
            if (player.getPlayerName().equals(playerName)) {
                player.setClientInterface(client);
                reconnectingPlayer = player; // save this player for later use
                break;
            }
        }

        GameSession sessionToRejoin = playerToSessionMap.get(playerName);
        if (sessionToRejoin != null) {

            // Reconnect the player to the GameSession
            System.out.println(playerName + " reconnected to the game session");
            sessionToRejoin.reconnectPlayer(client, playerName);

        } else {
            // Handle the case where there is no corresponding GameSession
            if (waitingPlayer == null) {
                // If there's no waiting player, set the reconnecting player as waitingPlayer
                waitingPlayer = reconnectingPlayer;
                waitingPlayer.getClientInterface().showWaitingScreen();
            } else {
                // If there's a waiting player, start a new game session
                setPlayer(client, playerName, false);
            }
        }
    }



    // Finds a player based on their client interface.
    private Player findPlayerByClientInterface(ClientInterface client) {
        for (Player p : allPlayers) {
            if (p.getClientInterface().equals(client)) {
                return p;
            }
        }
        return null;
    }
    // Handles client disconnection by setting a timer.
    public void handleClientDisconnection(ClientInterface client) {
        Player disconnectingPlayer = findPlayerByClientInterface(client);

        if (waitingPlayer != null && waitingPlayer.equals(disconnectingPlayer)) {
            waitingPlayer = null;
        }

        if (disconnectingPlayer != null) {
            disconnectPlayers.add(disconnectingPlayer.getPlayerName());

            GameSession affectedSession = playerToSessionMap.get(disconnectingPlayer.getPlayerName());
            if (affectedSession != null) {
                affectedSession.playerDisconnected(client);
            }

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (disconnectPlayers.contains(disconnectingPlayer.getPlayerName())) {
                        // 如果玩家在30秒内未重新连接
                        disconnectPlayers.remove(disconnectingPlayer.getPlayerName());

                        // 检查是否有与此玩家关联的GameSession
                        GameSession sessionToCheck = playerToSessionMap.get(disconnectingPlayer.getPlayerName());
                        if (sessionToCheck != null) {
                            // 将此对局标记为平局，并通知游戏中的另一名玩家
                            Player otherPlayer = sessionToCheck.getOtherPlayer(disconnectingPlayer);
                            try {
                                otherPlayer.getClientInterface().heartbeat();  // 或其他简单的远程方法
                                otherPlayer.getClientInterface().gameEnded("The game is a draw because " + disconnectingPlayer.getPlayerName() + " did not reconnect in time.");
                            } catch (RemoteException e) {
                                System.err.println("Other player is offline");
                            }
                            // 从活跃会话中移除这个GameSession
                            activeSessions.remove(sessionToCheck);
                            playerToSessionMap.remove(disconnectingPlayer.getPlayerName());
                            playerToSessionMap.remove(otherPlayer.getPlayerName());
                        }
                    }
                }
            }, 30000);  // 30 seconds
            disconnectionTimers.put(client, timer);
        }
    }



    // Called when a game session finishes to remove it from active sessions.
    public void gameFinished(GameSession session) throws RemoteException {
        // 你的具体实现，例如：
        // Remove the session from the list of active sessions
        activeSessions.remove(session);

    }
    // Returns the rank of a client in the game.
    @Override
    public int getRankOfClient(ClientInterface client) throws RemoteException {
        Player p = findPlayerByClientInterface(client);
        if (p != null) {
            return getRankOfPlayer(p);
        }
        return -1; // 返回-1表示错误或玩家未找到
    }
    public int getRankOfPlayer(Player player) {

        List<Player> sortedPlayers = new ArrayList<>(allPlayers);

        sortedPlayers.sort((p1, p2) -> {
            if (p1.getPoints() == p2.getPoints()) {
                return p1.getPlayerName().compareTo(p2.getPlayerName());
            }
            return Integer.compare(p2.getPoints(), p1.getPoints());
        });

        return sortedPlayers.indexOf(player) + 1;
    }


    // Quits a client from the game.
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

        if (waitingPlayer != null && waitingPlayer.equals(quittingPlayer)) {
            waitingPlayer = null;
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


    // Makes a move in the game on behalf of a client.
    public void makeMove(ClientInterface client, int row, int col) {
        try {
            GameSession session = findSessionByPlayer(client);
            if (session.isFrozen()) {
                client.displayNotification("The game is currently frozen due to a disconnected player. If the player doesn't reconnect within 30 seconds, the game will be declared a draw.");
                return;
            }
            if (session == null) {
                client.displayNotification("No active game session found for this player. Please reconnect.");
                return;
            }

            Player currentplayer = session.getCurrentPlayer();
            char symbol = currentplayer.getSymbol();
            try {
                session.getPlayer1().getClientInterface().heartbeat();
            } catch (RemoteException e) {
                handleClientDisconnection(session.getPlayer1().getClientInterface());
            }
            try {
                session.getPlayer2().getClientInterface().heartbeat();
            } catch (RemoteException e) {
                handleClientDisconnection(session.getPlayer2().getClientInterface());
            }

            Player player = findPlayerByClient(client, session);
            Player nextPlayer = session.makeMove(player, row, col);  // 获取返回的玩家
            int rank = getRankOfPlayer(nextPlayer);

            // Update info for both players
            Player otherPlayer = (nextPlayer == session.getPlayer1()) ? session.getPlayer2() : session.getPlayer1();
            nextPlayer.getClientInterface().updateCurrentPlayerInfo(nextPlayer.getPlayerName(), nextPlayer.getSymbol(), rank);
            otherPlayer.getClientInterface().updateCurrentPlayerInfo(nextPlayer.getPlayerName(), nextPlayer.getSymbol(), rank);
        } catch (Exception e) {
            try {
                client.displayNotification("An error occurred");
            } catch (RemoteException re) {
                System.err.println("Failed to send error notification to client" );
            }
        }
    }




    // Finds the game session associated with a player.
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


    // Sends a message to the other player in the game session.
    @Override
    public void sendMessage(ClientInterface sender, String message) throws RemoteException {
        GameSession game = findSessionByPlayer(sender);
        if (game.isFrozen()) {
            sender.displayNotification("The game is currently frozen due to a disconnected player.");
            return;
        }
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
    // Heartbeat method for the server.
    @Override
    public void heartbeat() {
    }





    // Starts the server and binds it to the provided RMI URL.
    public void start(String rmiUrl) {
        try {
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();

            // Add a debug print statement before rebind
            System.out.println("Attempting to bind to RMI registry with URL: " + rmiUrl);

            Naming.rebind(rmiUrl, this);

            // Add another debug print statement after rebind to confirm success
            System.out.println("Successfully bound to RMI registry.");

            System.out.println("Server ready");
        } catch (RemoteException e) {
            System.err.println("Remote exception while binding to RMI registry");
        } catch (MalformedURLException e) {
            System.err.println("The provided RMI URL is malformed. Please check your input.");
        }
    }
    // The main method to start the server.
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java YourServerClass ip port");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            TicTacToeServer server = new TicTacToeServer();
            String rmiUrl = "rmi://" + ip + ":" + port + "/TicTacToe";
            System.out.println(rmiUrl);
            server.start(rmiUrl);
        } catch (RemoteException e) {
            System.err.println("Server init error. Please ensure RMI setup is correct.");
        }
    }




}
