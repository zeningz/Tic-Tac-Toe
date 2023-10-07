package share;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {
    void updateGame(char[][] board) throws RemoteException;
    void receiveMessage(String message) throws RemoteException;
    void waitForOpponent() throws RemoteException;
    void startGame() throws RemoteException;


}
