import java.rmi.RemoteException;

public class GameSession {
    private Player player1;
    private Player player2;
    private char[][] board;
    private Player currentPlayer;

    public GameSession(Player p1, Player p2) {
        this.player1 = p1;
        this.player2 = p2;
        this.board = new char[3][3];
        updateClientsBoard();
        if (player1.getSymbol() == 'X') {
            notifyPlayerToMove(player1);
        } else {
            notifyPlayerToMove(player2);
        }
    }
    private void updateClientsBoard() {
        // Send the current board state to both players
        try {
            player1.getClientInterface().updateGame(board);
            player2.getClientInterface().updateGame(board);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    private void notifyPlayerToMove(Player player) {
        try {
            player.getClientInterface().yourTurn();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public enum GameState {
        ONGOING, FINISHED
    }
    private GameState gameState = GameState.ONGOING;
    public void makeMove(Player player, int row, int col) {
        // ... your move logic ...

        if (isGameFinished()) {
            gameState = GameState.FINISHED;
            notifyServerGameFinished();
        }
    }
    private boolean isGameFinished() {
        // ... check if the game is finished logic ...

        return true; // or false
    }
    public boolean hasPlayer(String playerName) {
        return (player1.getPlayerName().equals(playerName) || player2.getPlayerName().equals(playerName));
    }

    private void notifyServerGameFinished() {
        // Here you need a way to communicate back to the TicTacToeServer to remove this session.
        // This can be done by passing a reference of the server to each GameSession or by using some other callback mechanism.
    }
    public String outcome() {
        // TODO: Check game outcome.
        return null;
    }

    public void switchTurns() {
        // TODO: Implement turn switching logic.
    }
}
