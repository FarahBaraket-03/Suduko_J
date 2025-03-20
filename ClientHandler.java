import java.rmi.RemoteException;

public class ClientHandler extends Thread {
    private SudokuGame game;
    private String clientId;

    public ClientHandler(SudokuGame game, String clientId) {
        this.game = game;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try {
            // Simulate client interaction
            String[] puzzle = game.getPuzzle(clientId);
            System.out.println("Puzzle assigned to client " + clientId);

            // Simulate a move
            boolean moveResult = game.makeMove(clientId, 0, 0, 3);
            System.out.println("Move result for client " + clientId + ": " + moveResult);

            // Check if the puzzle is solved
            boolean isSolved = game.isSolved(clientId);
            System.out.println("Puzzle solved by client " + clientId + ": " + isSolved);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}