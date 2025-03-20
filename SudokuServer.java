import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

public class SudokuServer extends UnicastRemoteObject implements SudokuGame {
    private String[][] puzzles;
    private String[][] solutions;
    private Map<String, String[]> clientPuzzles = new HashMap<>();
    private Map<String, Integer> clientPuzzleIndices = new HashMap<>();
    private Map<String, SudokuCallback> clientCallbacks = new HashMap<>();
    private int currentPuzzleIndex = 0;
    private int clientCount = 0; // Track the number of connected clients
    private static final int MAX_CLIENTS = 10; // Maximum number of clients allowed

    public SudokuServer() throws RemoteException {
        super();
        puzzles = SudokuFactory.createPuzzles();
        solutions = SudokuFactory.createSolutions();
    }

    @Override
    public synchronized String[] getPuzzle(String clientId) throws RemoteException {
        // Check if the maximum number of clients has been reached
        if (clientCount >= MAX_CLIENTS) {
            throw new RemoteException("Server is full. Maximum number of clients reached.");
        }

        // Assign a unique puzzle index for each client
        int puzzleIndex = currentPuzzleIndex % puzzles.length;
        currentPuzzleIndex++;
        String[] puzzle = puzzles[puzzleIndex].clone();
        clientPuzzles.put(clientId, puzzle);
        clientPuzzleIndices.put(clientId, puzzleIndex);
        clientCount++; // Increment the client counter
        System.out.println("New client connected: " + clientId + ". Total clients: " + clientCount);
        return puzzle; // Send a new puzzle to the client
    }

    @Override
    public synchronized boolean makeMove(String clientId, int row, int col, int number) throws RemoteException {
        if (!clientPuzzles.containsKey(clientId) ){
            return false; // Client not registered
        }

        // Get the client's assigned puzzle index
        int puzzleIndex = clientPuzzleIndices.get(clientId);
        String[] clientPuzzle = clientPuzzles.get(clientId);

        // Check against the correct solution
        char solutionChar = solutions[puzzleIndex][row].charAt(col);
        if (solutionChar == (char) (number + '0')) {
            clientPuzzle[row] = clientPuzzle[row].substring(0, col) + number + clientPuzzle[row].substring(col + 1);
            return true;
        } else {
            // Notify error through callback
            SudokuCallback callback = clientCallbacks.get(clientId);
            if (callback != null) {
                try {
                    callback.notifyError("Invalid move! Try again.");
                } catch (RemoteException e) {
                    System.err.println("Error notifying client: " + e.getMessage());
                }
            }
            return false;
        }
    }

    @Override
    public synchronized boolean isSolved(String clientId) throws RemoteException {
        if (!clientPuzzles.containsKey(clientId)) {
            return false;
        }

        int puzzleIndex = clientPuzzleIndices.get(clientId);
        String[] clientPuzzle = clientPuzzles.get(clientId);

        for (int i = 0; i < 9; i++) {
            if (!clientPuzzle[i].equals(solutions[puzzleIndex][i])) {
                return false;
            }
        }

        SudokuCallback callback = clientCallbacks.get(clientId);
        if (callback != null) {
            callback.notifyCompletion();
        }

        // Decrement the client counter when the puzzle is solved
        clientCount--;
        System.out.println("Client " + clientId + " solved the puzzle. Total clients: " + clientCount);

        // Clean up client data
        clientPuzzles.remove(clientId);
        clientPuzzleIndices.remove(clientId);
        clientCallbacks.remove(clientId);

        return true;
    }

    @Override
    public synchronized void registerCallback(String clientId, SudokuCallback callback) throws RemoteException {
        clientCallbacks.put(clientId, callback);
        System.out.println("Callback registered for client: " + clientId);
    }

    public static void main(String[] args) {
        try {
            //  String serverIP = " 0.0.0.0"; // Bind to all available network interfaces
            //  System.setProperty("java.rmi.server.hostname", serverIP);
            SudokuServer server = new SudokuServer();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("SudokuGame", server);
            System.out.println("Sudoku Server is ready.");

            // Add a shutdown hook to gracefully handle server shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down the server...");
                try {
                    registry.unbind("SudokuGame");
                } catch (Exception e) {
                    System.err.println("Error during server shutdown: " + e.getMessage());
                }
            }));

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}