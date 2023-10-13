import share.ClientInterface;

public class Player {
    // Instance variables to represent player details.
    private String playerName;
    private char symbol;
    private ClientInterface clientInterface;
    private int points;
    /**
     * Constructor to initialize a player with a name, symbol, and client interface.
     */
    public Player(String playerName, char symbol, ClientInterface client) {
        this.playerName = playerName;
        this.symbol = symbol;
        this.clientInterface = client;
        this.points = 0; /// Each player starts with 0 points.
    }
    // Setter for updating the client interface (useful if a player reconnects).
    public void setClientInterface(ClientInterface clientInterface) {
        this.clientInterface = clientInterface;
    }
    // Getter for player's name.
    public String getPlayerName() {
        return playerName;
    }
    // Getter for player's symbol (either 'X' or 'O').
    public char getSymbol() {
        return this.symbol;
    }
    // Setter for player's symbol.
    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }


    // Getter for client interface.
    public ClientInterface getClientInterface() {
        return this.clientInterface;
    }
    /**
     * Method to add (or subtract) points to the player.
     * The player's points cannot go below 0.
     */
    public void addPoints(int points) {
        this.points = Math.max(0, this.points + points);
    }
    // Getter for player's points.
    // 新增：获取玩家的分数
    public int getPoints() {
        return this.points;
    }

}
