import java.rmi.Naming;
import java.rmi.RemoteException;
import share.ClientInterface;
import share.ServerInterface;
import java.rmi.server.UnicastRemoteObject;

public class Client implements ClientInterface {
    private ClientInterface clientStub;
    private ServerInterface server;
    private ClientGUI gui;

    public Client(String serverURL) {
        try {
            clientStub = (ClientInterface) UnicastRemoteObject.exportObject(this, 0);
            server = (ServerInterface) Naming.lookup(serverURL);
            gui = new ClientGUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean connectToServer(String playerName) {
        System.out.println("Attempting to connect with name: " + playerName);
        try {
            server.setPlayer(clientStub, playerName);
            return true;
        } catch (RemoteException e) {
            if (e.getMessage().contains("Name alrsseady in use")) {  // 修改这里，使用contains而不是equals
                gui.displayError("Name already in use. Please choose a different name.");
                return false;
            } else {
                e.printStackTrace();
                return false;
            }
        }
    }



    public void makeMove(int row, int col) {
        try {
            server.makeMove(clientStub, row, col);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void quit() {
        try {
            server.quit(clientStub);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    public void sendMessage(String message) {
        try {
            server.sendMessage(clientStub, message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessage(String message) {
        gui.updateChat(message);
    }

    public void updateGame(char[][] board) {
        gui.updateBoard(board);
    }

    public static void main(String[] args) {
        new Client("rmi://localhost/TicTacToe");
    }
}
