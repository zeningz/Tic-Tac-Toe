import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServerMain {
    public static void main(String[] args) {
        try {
            // 创建并启动RMI注册表
            LocateRegistry.createRegistry(1099);

            // 创建服务器实例
            TicTacToeServer server = new TicTacToeServer();

            // 注册服务器实例
            Naming.rebind("rmi://localhost/TicTacToe", server);

            System.out.println("TicTacToeServer is ready.");
        } catch (Exception e) {
            System.err.println("TicTacToeServer exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
