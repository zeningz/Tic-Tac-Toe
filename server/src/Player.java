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

    // TODO: Add getters, setters, and other necessary methods.
}
