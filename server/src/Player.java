import share.ClientInterface;

import java.rmi.RemoteException;

public class Player {
    private String playerName;
    private char symbol;
    private ClientInterface clientInterface;
    private int points;

    public Player(String playerName, char symbol, ClientInterface client) {
        this.playerName = playerName;
        this.symbol = symbol;
        this.clientInterface = client;
        this.points = 0; // 每个玩家开始时的分数都是0
    }
    public void setClientInterface(ClientInterface clientInterface) {
        this.clientInterface = clientInterface;
    }

    public String getPlayerName() {
        return playerName;
    }

    public char getSymbol() {
        return this.symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }

    public void notifyGameResult(String message) {
        try {
            clientInterface.displayNotification(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public ClientInterface getClientInterface() {
        return this.clientInterface;
    }

    // 新增：增加玩家的分数
    public void addPoints(int points) {
        this.points = Math.max(0, this.points + points);
    }

    // 新增：获取玩家的分数
    public int getPoints() {
        return this.points;
    }

    // TODO: Add getters, setters, and other necessary methods.
}
