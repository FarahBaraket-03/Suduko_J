import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RMIClassLoader;
import java.net.URL;

public class SudokuServer {
    public static void main(String[] args) {
        try {
            // Set security policy
            System.setProperty("java.security.policy", "server.policy");
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }
            
            // Set codebase for dynamic class loading
            System.setProperty("java.rmi.server.codebase", "http://192.168.56.1/classes/");
            
            // Verify codebase is accessible
            try {
                new URL(System.getProperty("java.rmi.server.codebase") + "FabSudokuInterface.class").openConnection().connect();
                System.out.println("Server codebase accessible");
            } catch (Exception e) {
                System.err.println("Cannot access server codebase: " + e.getMessage());
                return;
            }

            // Create and export the factory
            FabSudokuImpl factory = new FabSudokuImpl();
            
            // Create registry
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Bind the factory
            registry.rebind("SudokuFactory", factory);
            
            System.out.println("Sudoku Factory created and Server ready");
            System.out.println("Codebase: " + System.getProperty("java.rmi.server.codebase"));
            
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}