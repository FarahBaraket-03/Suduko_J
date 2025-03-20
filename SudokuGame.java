import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SudokuGame extends Remote {
    String[] getPuzzle(String clientId) throws RemoteException;
    boolean makeMove(String clientId, int row, int col, int number) throws RemoteException;
    boolean isSolved(String clientId) throws RemoteException;
    void registerCallback(String clientId, SudokuCallback callback) throws RemoteException;
}