import java.rmi.RemoteException;
import share.ClientInterface;

public class GameSession {
    private Player player1;
    private Player player2;
    private char[][] board;
    private Player currentPlayer;
    private TicTacToeServer server;

    private boolean isFrozen = false;
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
    private void updateClientsBoard() throws RemoteException{
        // Send the current board state to both players

        ClientInterface client1 = player1.getClientInterface();
        ClientInterface client2 = player2.getClientInterface();
        client1.updateGame(board);
        client2.updateGame(board);

    }


    public enum GameState {
        ONGOING, FINISHED
    }
    private GameState gameState = GameState.ONGOING;
    private final Object moveLock = new Object();

    public synchronized Player makeMove(Player player, int row, int col) throws RemoteException {

        synchronized(moveLock) {
            if (isFrozen) {
                // 不执行移动，返回错误消息
                return null;
            }
            if (player != currentPlayer) {
                return null;
            }

            board[row][col] = player.getSymbol();
            updateClientsBoard();

            try {
                Thread.sleep(100);  // 延迟100毫秒
            } catch (InterruptedException e) {
                System.err.println("Fail to move, Check gamesession");
            }


            if (isWinner(player.getSymbol())) {
                player.addPoints(5); // 赢家得到5分
                // 获取对手并减去5分
                Player opponent = (player == player1) ? player2 : player1;
                opponent.addPoints(-5);
                gameState = GameState.FINISHED;
                updateClientsBoard();
                notifyServerGameFinished();
                notifyPlayers(player.getPlayerName() + " wins!");
                return currentPlayer;
            } else if (isDraw()) {
                player1.addPoints(2); // 平局，每个玩家得到2分
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

    public boolean isFrozen() {
        return isFrozen;
    }
    public void playerReconnected(ClientInterface client) {
        if (server.areBothPlayersConnected(this)) { // 判断两个玩家是否都已连接
            isFrozen = false;
        }
    }
    public Player getOtherPlayer(Player currentPlayer) {
        return currentPlayer.equals(player1) ? player2 : (currentPlayer.equals(player2) ? player1 : null);
    }
    public void playerDisconnected(ClientInterface client) {
        isFrozen = true;
    }
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


    public boolean isWinner(char symbol) {
        // 检查行、列和对角线
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

    public boolean isDraw() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    return false;  // 还有空位
                }
            }
        }
        return true;  // 没有空位，平局
    }

    public boolean hasPlayer(ClientInterface client) {
        return player1.getClientInterface().equals(client) || player2.getClientInterface().equals(client);
    }


    public void startGame() throws RemoteException {
        System.out.println("startGame");
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

    private void notifyServerGameFinished() throws RemoteException{
        try {
            // Inform the server that this game session has ended.
            // Assuming you have a method in your server interface to handle game session end.
            server.gameFinished(this);
        } catch (RemoteException e) {
            System.err.println("Error notifying server about game finish");

        }
    }

    public char[][] getBoardState() throws RemoteException{
        // 假设你有一个char[][]来代表棋盘状态
        return this.board;
    }
    public void reconnectPlayer(ClientInterface client, String playerName) throws RemoteException {
        Player playerToReconnect = (player1.getPlayerName().equals(playerName)) ? player1 : player2;
        Player otherPlayer = (playerToReconnect == player1) ? player2 : player1;

        // 更新重连玩家的ClientInterface
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


    private Player switchTurns() throws RemoteException{
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        return currentPlayer;

    }
}
