package share;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {


    boolean setPlayer(ClientInterface client, String playerName, boolean checkWaitingPlayer) throws RemoteException;


    void quit(ClientInterface client) throws RemoteException;
    void makeMove(ClientInterface client, int row, int col) throws RemoteException;
    void sendMessage(ClientInterface sender, String message) throws RemoteException;
    char[][] getBoard(ClientInterface client) throws RemoteException;

}
