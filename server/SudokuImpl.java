import java.rmi.*;
import java.rmi.server.*;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class SudokuImpl extends UnicastRemoteObject implements SudokuInterface {
    private final String clientId;
    private final String[] puzzle;
    private final String[] solution;
    private volatile SudokuCallback callback;
    private final ReentrantLock lock = new ReentrantLock();
    private static final Random random = new Random();

    public SudokuImpl(String clientId) throws RemoteException {
        super();
        this.clientId = clientId;
        String[][] puzzles = SudokuFactory.createPuzzles();
        String[][] solutions = SudokuFactory.createSolutions();
        int index = random.nextInt(puzzles.length);
        this.puzzle = puzzles[index].clone();
        this.solution = solutions[index].clone();
    }

    @Override
    public String[] getPuzzle() throws RemoteException {
        lock.lock();
        try {
            return puzzle.clone();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean makeMove(int row, int col, int number) throws RemoteException {
        if (row < 0 || row >= 9 || col < 0 || col >= 9 || number < 1 || number > 9) {
            notifyErrorSafe("Invalid move coordinates");
            return false;
        }

        lock.lock();
        try {
            if (solution[row].charAt(col) == (char)(number + '0')) {
                puzzle[row] = puzzle[row].substring(0, col) + number + puzzle[row].substring(col + 1);
                if (isSolved()) {
                    notifyCompletionSafe();
                }
                return true;
            }
            notifyErrorSafe("Invalid move at row " + (row+1) + ", column " + (col+1));
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isSolved() throws RemoteException {
        lock.lock();
        try {
            for (int i = 0; i < 9; i++) {
                if (!puzzle[i].equals(solution[i])) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized void registerCallback(SudokuCallback callback) throws RemoteException {
        // Clear previous callback reference
        this.callback = null;
        
        // Set new callback
        if (callback != null) {
            this.callback = callback;
        }
    }

    private void notifyCompletionSafe() {
        SudokuCallback cb = this.callback;
        if (cb != null) {
            try {
                cb.notifyCompletion();
            } catch (RemoteException e) {
                System.err.println("Error notifying completion: " + e.getMessage());
                this.callback = null;
            }
        }
    }

    private void notifyErrorSafe(String message) {
        SudokuCallback cb = this.callback;
        if (cb != null) {
            try {
                cb.notifyError(message);
            } catch (RemoteException e) {
                System.err.println("Error notifying error: " + e.getMessage());
                this.callback = null;
            }
        }
    }

    public synchronized void cleanup() {
        this.callback = null;
    }
}