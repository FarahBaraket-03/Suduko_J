import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

public class SudokuClient implements SudokuCallback {
    private SudokuGame game;
    private String clientId;
    private JFrame frame;
    private JLabel textLabel;
    private JPanel boardPanel;
    private JPanel buttonsPanel;
    private JButton numSelected;
    private int errors = 0;

    public SudokuClient(String clientId, String serverIP) {
        this.clientId = clientId;
        try {
            Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
            game = (SudokuGame) registry.lookup("SudokuGame");

            // Register the callback
            SudokuCallback callback = (SudokuCallback) UnicastRemoteObject.exportObject(this, 0);
            game.registerCallback(clientId, callback);
            String[] puzzle = game.getPuzzle(clientId);
            initializeGUI(puzzle);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private void initializeGUI(String[] puzzle) {
        frame = new JFrame("Sudoku");
        frame.setSize(600, 650);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        textLabel = new JLabel();
        textLabel.setFont(new Font("Arial", Font.BOLD, 30));
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setText("Sudoku: 0");

        JPanel textPanel = new JPanel();
        textPanel.add(textLabel);
        frame.add(textPanel, BorderLayout.NORTH);

        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(9, 9));
        setupTiles(puzzle);
        frame.add(boardPanel, BorderLayout.CENTER);

        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 9));
        setupButtons();
        frame.add(buttonsPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

  private void setupTiles(String[] puzzle) {
    for (int r = 0; r < 9; r++) {
        for (int c = 0; c < 9; c++) {
            JButton tile = new JButton();
            char tileChar = puzzle[r].charAt(c);
            if (tileChar != '-') {
                tile.setFont(new Font("Arial", Font.BOLD, 20));
                tile.setText(String.valueOf(tileChar));
                tile.setBackground(Color.lightGray);
            } else {
                tile.setFont(new Font("Arial", Font.PLAIN, 20));
                tile.setBackground(Color.white);
            }
            tile.setFocusable(false);
            boardPanel.add(tile);

            tile.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (numSelected != null) {
                        JButton tile = (JButton) e.getSource();
                        int row = boardPanel.getComponentZOrder(tile) / 9;
                        int col = boardPanel.getComponentZOrder(tile) % 9;
                        int number = Integer.parseInt(numSelected.getText());
                        try {
                            if (game.makeMove(clientId, row, col, number)) {
                                tile.setText(String.valueOf(number));
                                if (game.isSolved(clientId)) {
                                    notifyCompletion();
                                }
                            } else {
                                errors += 1;
                                textLabel.setText("Sudoku: " + errors);
                            }
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }
    }
     boardPanel.revalidate();
    boardPanel.repaint();
}
    private void setupButtons() {
        for (int i = 1; i < 10; i++) {
            JButton button = new JButton();
            button.setFont(new Font("Arial", Font.BOLD, 20));
            button.setText(String.valueOf(i));
            button.setFocusable(false);
            button.setBackground(Color.white);
            buttonsPanel.add(button);

            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (numSelected != null) {
                        numSelected.setBackground(Color.white);
                    }
                    numSelected = (JButton) e.getSource();
                    numSelected.setBackground(Color.lightGray);
                }
            });
        }
    }

    // Method to reset the game and fetch a new puzzle
  
private void resetGame() throws RemoteException {
    try {
        // Clear the existing board
        boardPanel.removeAll(); // Removes all tiles from the board

        // Reset error counter
        errors = 0;
        textLabel.setText("Sudoku: " + errors);

        // Fetch a new puzzle from the server
        String[] newPuzzle = game.getPuzzle(clientId);

        // Recreate the board with the new puzzle
        setupTiles(newPuzzle);

        // Force UI update
        SwingUtilities.invokeLater(() -> {
            boardPanel.revalidate();
            boardPanel.repaint(); // Ensure the UI is properly updated
        });
    } catch (RemoteException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(frame, "Error resetting game: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}


    @Override
    public void notifyError(String message) throws RemoteException {
        System.out.println("Error received: " + message); // Log the message
        SwingUtilities.invokeLater(() -> {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    });
    }

  private boolean isCompletionDialogShown = false; // Flag to prevent multiple dialogs

@Override
public void notifyCompletion() throws RemoteException {
    if (isCompletionDialogShown) {
        return; // Prevent multiple dialogs
    }

    isCompletionDialogShown = true; // Set the flag to true

    SwingUtilities.invokeLater(() -> {
        // Show the play again dialog
        int choice = JOptionPane.showConfirmDialog(frame, 
            "Congratulations! You solved the puzzle!\nDo you want to play again?", 
            "Game Over", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            // Use SwingWorker to reset the game asynchronously
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    resetGame(); // Reset the game
                    return null;
                }

                @Override
                protected void done() {
                    isCompletionDialogShown = false; // Reset the flag after resetting the game
                }
            }.execute();
        } else {
            System.exit(0); // Exit if "No" is selected
        }
    });
}

    public static void main(String[] args) {
    //      if (args.length < 1) {
    //     System.err.println("Usage: java SudokuClient <server-ip>");
    //     System.exit(1);
    // }

    String serverIP = "localhost";
        String clientId = "Client" + System.currentTimeMillis();
        new SudokuClient(clientId,serverIP);
    }
}