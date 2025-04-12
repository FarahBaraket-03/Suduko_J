import java.rmi.*;
import java.rmi.server.*;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class SudokuImpl extends UnicastRemoteObject implements SudokuInterface {
    private String clientId;
    private String[] puzzle;
    private String[] solution;
    private SudokuCallback callback;
    private final ReentrantLock lock = new ReentrantLock();
    private static final Random random = new Random();

    public SudokuImpl(String clientId) throws RemoteException {
        super();
        this.clientId = clientId;
        String[][] puzzles = SudokuFactory.createPuzzles();
        String[][] solutions = SudokuFactory.createSolutions();
        int index = random.nextInt(puzzles.length);
        this.puzzle = puzzles[index].clone(); // Create copy for each client
        this.solution = solutions[index];
    }

    @Override
    public String[] getPuzzle() throws RemoteException {
        return puzzle.clone(); // Return copy
    }

    @Override
    public boolean makeMove(int row, int col, int number) throws RemoteException {
        lock.lock();
        try {
            if (solution[row].charAt(col) == (char)(number + '0')) {
                puzzle[row] = puzzle[row].substring(0, col) + number + puzzle[row].substring(col + 1);
                if (isSolved() && callback != null) {
                    callback.notifyCompletion();
                }
                return true;
            }
            if (callback != null) {
                callback.notifyError("Invalid move at row " + (row+1) + ", column " + (col+1));
            }
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
    public void registerCallback(SudokuCallback callback) throws RemoteException {
        this.callback = callback;
    }
}