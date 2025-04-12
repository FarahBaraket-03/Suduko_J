import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SudokuCallback extends Remote {
    void notifyError(String message) throws RemoteException;
    void notifyCompletion() throws RemoteException;
}