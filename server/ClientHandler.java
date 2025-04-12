
import java.rmi.*;
import java.rmi.registry.*;



public class ClientHandler extends Thread {
    private String clientId;
    private String serverIP;

    public ClientHandler(String clientId, String serverIP) {
        this.clientId = clientId;
        this.serverIP = serverIP;
    }

    @Override
    public void run() {
        try {
            // Get the factory
            Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
            FabSudokuInterface factory = (FabSudokuInterface) registry.lookup("SudokuFactory");
            
            // Create new game instance
            SudokuInterface game = factory.newSudoku(clientId);
            
            // Simulate client interaction
            String[] puzzle = game.getPuzzle();
            System.out.println("Puzzle assigned to client " + clientId);

            // Simulate a move
            boolean moveResult = game.makeMove(0, 0, 3);
            System.out.println("Move result for client " + clientId + ": " + moveResult);

            // Check if solved
            boolean isSolved = game.isSolved();
            System.out.println("Puzzle solved by client " + clientId + ": " + isSolved);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}