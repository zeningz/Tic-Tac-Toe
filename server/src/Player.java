import share.ClientInterface;
public class Player {
    private String playerName;
    private char symbol;
    private ClientInterface clientInterface;
    private int points;

    public Player(String playerName, char symbol, ClientInterface client) {
        this.playerName = playerName;
        this.symbol = symbol;
        this.clientInterface = client;
        this.points = 0;
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
    public ClientInterface getClientInterface() {
        return this.clientInterface;
    }
    // TODO: Add getters, setters, and other necessary methods.
}
