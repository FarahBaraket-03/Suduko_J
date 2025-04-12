
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SudokuInterface extends Remote {
    String[] getPuzzle() throws RemoteException;
    boolean makeMove(int row, int col, int number) throws RemoteException;
    boolean isSolved() throws RemoteException;
    void registerCallback(SudokuCallback callback) throws RemoteException;
}