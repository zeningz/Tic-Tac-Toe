package share;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {
    void updateGame(char[][] board) throws RemoteException;
    void receiveMessage(String message) throws RemoteException;
    void waitForOpponent() throws RemoteException;
    void startGame() throws RemoteException;
    public void setPlayerDetails(String playerName, char playerSymbol) throws RemoteException;

    void createGameFrame() throws RemoteException;
    void gameEnded(String winnerMessage) throws RemoteException;

    void heartbeat() throws RemoteException;;

    void updateCurrentPlayerInfo(String playerName, char symbol, int rank) throws RemoteException;

    void displayNotification(String message) throws RemoteException;
}
