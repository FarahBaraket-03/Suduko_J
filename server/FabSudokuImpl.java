import java.rmi.*;
import java.rmi.server.*;
import java.util.concurrent.ConcurrentHashMap;

public class FabSudokuImpl extends UnicastRemoteObject implements FabSudokuInterface {
    private final ConcurrentHashMap<String, SudokuInterface> activeGames;
    private static final int MAX_CLIENTS = 10; // Adjust as needed
    
    public FabSudokuImpl() throws RemoteException {
        super();
        this.activeGames = new ConcurrentHashMap<>();
    }
    
    public synchronized SudokuInterface newSudoku(String clientId) throws RemoteException {
        if (activeGames.size() >= MAX_CLIENTS) {
            throw new RemoteException("Server busy. Maximum clients reached. Try again later.");
        }
        
        SudokuInterface game = new SudokuImpl(clientId);
        activeGames.put(clientId, game);
        System.out.println("New client connected: " + clientId + ". Active clients: " + activeGames.size());
        return game;
    }
    
    public synchronized void removeClient(String clientId) throws RemoteException {
        SudokuInterface game = activeGames.remove(clientId);
        if (game != null) {
            System.out.println("Client " + clientId + " removed. Active clients: " + activeGames.size());
        }
    }
}