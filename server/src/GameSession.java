import java.rmi.RemoteException;
import share.ClientInterface;

public class GameSession {
    // Instance variables to represent the players, game board, and server details.
    private Player player1;
    private Player player2;
    private char[][] board;
    private Player currentPlayer;
    private TicTacToeServer server;

    private boolean isFrozen = false;
    /**
     * Constructor for the GameSession class. Initializes a new game session with two players.
     */
    public GameSession(Player p1, Player p2,TicTacToeServer server) {
        this.player1 = p1;
        this.player2 = p2;
        this.server = server;
        this.board = new char[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
        if (player1.getSymbol() == 'X') {
            currentPlayer = player1;
        } else {
            currentPlayer = player2;
        }

    }
    /**
     * This method sends the current game board's state to both players.
     */
    private void updateClientsBoard() throws RemoteException{
        // Send the current board state to both players

        ClientInterface client1 = player1.getClientInterface();
        ClientInterface client2 = player2.getClientInterface();
        client1.updateGame(board);
        client2.updateGame(board);

    }

    /**
     * Enum for representing the current state of the game.
     */
    public enum GameState {
        ONGOING, FINISHED
    }
    private GameState gameState = GameState.ONGOING;
    private final Object moveLock = new Object();
    /**
     * This synchronized method allows a player to make a move, and then updates the game state and board.
     * It also checks if the game has reached a win or draw state.
     */
    public synchronized Player makeMove(Player player, int row, int col) throws RemoteException {

        synchronized(moveLock) {
            if (isFrozen) {
                return null;
            }
            if (player != currentPlayer) {
                return null;
            }

            board[row][col] = player.getSymbol();
            updateClientsBoard();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.err.println("Fail to move, Check gamesession");
            }


            if (isWinner(player.getSymbol())) {
                player.addPoints(5);

                Player opponent = (player == player1) ? player2 : player1;
                opponent.addPoints(-5);
                gameState = GameState.FINISHED;
                updateClientsBoard();
                notifyServerGameFinished();
                notifyPlayers(player.getPlayerName() + " wins!");
                return currentPlayer;
            } else if (isDraw()) {
                player1.addPoints(2);
                player2.addPoints(2);
                gameState = GameState.FINISHED;
                updateClientsBoard();
                notifyServerGameFinished();
                notifyPlayers("It's a draw!");
                return currentPlayer;
            } else {
                return switchTurns();
            }

        }
    }
    /**
     * Returns true if the game is currently frozen (i.e., due to a player's disconnection).
     */
    public boolean isFrozen() {
        return isFrozen;
    }
    /**
     * This method handles a player's reconnection event.
     */
    public void playerReconnected(ClientInterface client) {
        if (server.areBothPlayersConnected(this)) { // 判断两个玩家是否都已连接
            isFrozen = false;
        }
    }
    /**
     * Returns the other player (not the current player).
     */
    public Player getOtherPlayer(Player currentPlayer) {
        return currentPlayer.equals(player1) ? player2 : (currentPlayer.equals(player2) ? player1 : null);
    }
    /**
     * Handles a player's disconnection event by setting the game's state to frozen.
     */
    public void playerDisconnected(ClientInterface client) {
        isFrozen = true;
    }
    /**
     * Notifies both players with a given message.
     */
    private void notifyPlayers(String message) throws RemoteException{
        Thread t1 = new Thread(() -> {
            try {
                player1.getClientInterface().gameEnded(message);
            } catch (RemoteException e) {
                System.err.println("Fail to notifyPlayers "+player1);
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                player2.getClientInterface().gameEnded(message);
            } catch (RemoteException e) {
                System.err.println("Fail to notifyPlayers "+player2);
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.err.println("Fail to use thread");
        }
    }

    /**
     * Checks if the given player symbol (X or O) has won the game.
     */
    public boolean isWinner(char symbol) {

        for (int i = 0; i < 3; i++) {
            if ((board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) ||
                    (board[0][i] == symbol && board[1][i] == symbol && board[2][i] == symbol)) {
                return true;
            }
        }
        if ((board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) ||
                (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol)) {
            return true;
        }
        return false;
    }
    /**
     * Checks if the game has reached a draw state.
     */
    public boolean isDraw() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * Checks if the specified client is one of the players in this game session.
     */
    public boolean hasPlayer(ClientInterface client) {
        return player1.getClientInterface().equals(client) || player2.getClientInterface().equals(client);
    }

    /**
     * Initiates the game by sending the initial state of the game board to both players.
     */
    public void startGame() throws RemoteException {
        updateClientsBoard();
    }
    public Player getPlayer1(){
        return player1;
    }
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    public Player getPlayer2() {
        return player2;
    }
    /**
     * Notifies the server that this game session has finished.
     */
    private void notifyServerGameFinished() throws RemoteException{
        try {
            // Inform the server that this game session has ended.
            // Assuming you have a method in your server interface to handle game session end.
            server.gameFinished(this);
        } catch (RemoteException e) {
            System.err.println("Error notifying server about game finish");

        }
    }
    /**
     * Returns the current state of the game board.
     */
    public char[][] getBoardState() throws RemoteException{
        // 假设你有一个char[][]来代表棋盘状态
        return this.board;
    }
    /**
     * Handles a player's reconnection event and updates the game state and UI accordingly.
     */
    public void reconnectPlayer(ClientInterface client, String playerName) throws RemoteException {
        Player playerToReconnect = (player1.getPlayerName().equals(playerName)) ? player1 : player2;
        Player otherPlayer = (playerToReconnect == player1) ? player2 : player1;
        playerReconnected(client);
        // 更新重连玩家的ClientInterface
        client.setPlayerDetails(playerToReconnect.getPlayerName(), playerToReconnect.getSymbol());
        playerToReconnect.setClientInterface(client);
        playerToReconnect.getClientInterface().createGameFrame();
        playerToReconnect.getClientInterface().startGame();

        char[][] boardState = getBoardState();
        Player currentPlayer = getCurrentPlayer();
        int rank = server.getRankOfPlayer(currentPlayer);
        // 更新重连玩家的游戏状态
        client.updateGame(boardState);
        client.setPlayerDetails(playerToReconnect.getPlayerName(), playerToReconnect.getSymbol());

        client.updateCurrentPlayerInfo(currentPlayer.getPlayerName(), currentPlayer.getSymbol(), rank);
        // 同时更新其他玩家的游戏状态
        otherPlayer.getClientInterface().updateGame(boardState);
        otherPlayer.getClientInterface().updateCurrentPlayerInfo(currentPlayer.getPlayerName(), currentPlayer.getSymbol(), rank);
    }

    /**
     * Switches the turn to the other player.
     */
    private Player switchTurns() throws RemoteException{
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        return currentPlayer;

    }
}
