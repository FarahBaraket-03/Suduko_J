
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.*;

public class FabSudokuImpl extends UnicastRemoteObject implements FabSudokuInterface {
    private final Map<String, SudokuInterface> activeGames = 
        Collections.synchronizedMap(new HashMap<String, SudokuInterface>());
    private final Set<String> activeClients = 
        Collections.synchronizedSet(new HashSet<String>());
    private static final int MAX_CLIENTS = 10;

    public FabSudokuImpl() throws RemoteException {
        super(); // This exports the object automatically
    }

    public synchronized SudokuInterface newSudoku(String clientId) throws RemoteException {
        if (activeClients.size() >= MAX_CLIENTS) {
            throw new RemoteException("Maximum clients reached (" + MAX_CLIENTS + ")");
        }
        
        if (activeClients.contains(clientId)) {
            throw new RemoteException("Client ID already exists");
        }

        // This creates and automatically exports the SudokuImpl instance
        SudokuInterface game = new SudokuImpl(clientId);
        activeGames.put(clientId, game);
        activeClients.add(clientId);
        System.out.println("Client connected: " + clientId + " (Total: " + activeClients.size() + ")");
        return game;
    }

    public synchronized void removeClient(String clientId) throws RemoteException {
        if (activeClients.remove(clientId)) {
            SudokuInterface game = activeGames.remove(clientId);
            // Unexport the game instance
            try {
                UnicastRemoteObject.unexportObject(game, true);
            } catch (Exception e) {
                System.err.println("Error unexporting game for client " + clientId + ": " + e.getMessage());
            }
            System.out.println("Client disconnected: " + clientId + " (Remaining: " + activeClients.size() + ")");
        }
    }
}