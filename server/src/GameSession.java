import java.rmi.RemoteException;
import share.ClientInterface;
import share.ServerInterface;

public class GameSession {
    private Player player1;
    private Player player2;
    private char[][] board;
    private Player currentPlayer;
    private TicTacToeServer server;

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
            if (player != currentPlayer) {
                return null;
            }

            board[row][col] = player.getSymbol();
            updateClientsBoard();

            try {
                Thread.sleep(100);  // 延迟100毫秒
            } catch (InterruptedException e) {
                e.printStackTrace();
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
    public void endGameAsDraw() throws RemoteException {
        player1.addPoints(2); // 平局，每个玩家得到2分
        player2.addPoints(2);
        gameState = GameState.FINISHED;
        updateClientsBoard();
        notifyPlayers("It's a draw due to disconnection!");
    }

    private void notifyPlayers(String message) throws RemoteException{
        Thread t1 = new Thread(() -> {
            try {
                player1.getClientInterface().gameEnded(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                player2.getClientInterface().gameEnded(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isGameFinished() {
        // Check for a winning line
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != ' ' && board[i][0] == board[i][1] && board[i][0] == board[i][2]) {
                return true; // row win
            }
            if (board[0][i] != ' ' && board[0][i] == board[1][i] && board[0][i] == board[2][i]) {
                return true; // column win
            }
        }
        // Check diagonals
        if (board[0][0] != ' ' && board[0][0] == board[1][1] && board[0][0] == board[2][2]) {
            return true; // top-left to bottom-right diagonal
        }
        if (board[0][2] != ' ' && board[0][2] == board[1][1] && board[0][2] == board[2][0]) {
            return true; // top-right to bottom-left diagonal
        }

        // Check for a tie (full board without a winner)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') { // found an empty spot
                    return false;
                }
            }
        }
        // It's a tie if the board is full and there's no winner
        return true;
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
    public Player getPlayer1() throws RemoteException{
        return player1;
    }
    public Player getCurrentPlayer() throws RemoteException {
        return currentPlayer;
    }
    public Player getPlayer2() throws RemoteException {
        return player2;
    }

    private void notifyServerGameFinished() throws RemoteException{
        try {
            // Inform the server that this game session has ended.
            // Assuming you have a method in your server interface to handle game session end.
            server.gameFinished(this);
        } catch (RemoteException e) {
            System.err.println("Error notifying server about game finish: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public String outcome() {
        // TODO: Check game outcome.
        return null;
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
