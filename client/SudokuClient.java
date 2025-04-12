import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.rmi.server.RMIClassLoader;

public class SudokuClient {
    private Remote game;
    private String clientId;
    private JFrame frame;
    private JLabel textLabel;
    private JPanel boardPanel;
    private JPanel buttonsPanel;
    private JButton numSelected;
    private int errors = 0;
    private String serverIP;
    private Remote callback;
    private boolean callbackExported = false;
    private boolean isCompletionDialogShown = false;

    public SudokuClient(String serverIP) {
        this.serverIP = serverIP;
        try {
            System.setProperty("java.security.policy", "client.policy");
            System.setProperty("java.rmi.server.codebase", "http://192.168.56.1/classes/");
            
            // Verify HTTP server accessibility
            System.out.println("Testing codebase accessibility...");
            try {
                URL url = new URL(System.getProperty("java.rmi.server.codebase") + "FabSudokuInterface.class");
                System.out.println("Trying to access: " + url);
                url.openConnection().connect();
                System.out.println("Codebase accessible");
            } catch (Exception e) {
                System.err.println("Cannot access codebase: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }
            
            this.clientId = "Client" + System.currentTimeMillis();
            initializeGame();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Connection error: " + e.getMessage());
            System.exit(1);
        }
    }
    

    private void initializeGUI(String[] puzzle) {
        // Clear existing frame if it exists
        if (frame != null) {
            frame.getContentPane().removeAll();
        } else {
            frame = new JFrame("Sudoku - " + clientId);
            frame.setSize(600, 650);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
        }
    
        frame.setLayout(new BorderLayout());
    
        textLabel = new JLabel();
        textLabel.setFont(new Font("Arial", Font.BOLD, 30));
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setText("Sudoku: " + errors);
    
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
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cleanup();
                System.exit(0);
            }
        });
    }
    
    private void setupTiles(String[] puzzle) {
        boardPanel.removeAll();
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
                                Class<?> gameInterface = RMIClassLoader.loadClass(
                                    System.getProperty("java.rmi.server.codebase"), 
                                    "SudokuInterface");
                                boolean validMove = (boolean) gameInterface
                                    .getMethod("makeMove", int.class, int.class, int.class)
                                    .invoke(game, row, col, number);
                                
                                if (validMove) {
                                    tile.setText(String.valueOf(number));
                                    boolean solved = (boolean) gameInterface
                                        .getMethod("isSolved")
                                        .invoke(game);
                                    if (solved) {
                                        Method notifyCompletion = callback.getClass()
                                            .getMethod("notifyCompletion");
                                        notifyCompletion.invoke(callback);
                                    }
                                } else {
                                    errors++;
                                    textLabel.setText("Sudoku: " + errors);
                                }
                            } catch (Exception ex) {
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

    private void cleanup() {
        try {
            // Unregister callback first
            unexportCallback();
            
            // Only try to remove client if we successfully created a game
            if (game != null) {
                Class<?> fabInterface = RMIClassLoader.loadClass(
                    System.getProperty("java.rmi.server.codebase"), 
                    "FabSudokuInterface");
                Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
                Remote factory = registry.lookup("SudokuFactory");
                
                // Use reflection more carefully
                Method removeMethod = fabInterface.getMethod("removeClient", String.class);
                removeMethod.invoke(factory, clientId);
            }
        } catch (Exception e) {
            System.err.println("Cleanup warning: " + e.getMessage());
        }
    }

   private void initializeGame() throws Exception {
    try {
        // Load interfaces using RMIClassLoader
        Class<?> fabInterface = RMIClassLoader.loadClass(
            System.getProperty("java.rmi.server.codebase"), 
            "FabSudokuInterface");
        
        Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
        Remote factory = registry.lookup("SudokuFactory");
        
        // Add retry logic for server busy situations
        int retries = 3;
        while (retries-- > 0) {
            try {
                this.game = (Remote) fabInterface.getMethod("newSudoku", String.class)
                                  .invoke(factory, clientId);
                if (game == null) {
                    throw new RemoteException("Failed to create game instance");
                }
                break;
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RemoteException && 
                    e.getCause().getMessage().contains("Maximum clients reached")) {
                    if (retries > 0) {
                        Thread.sleep(2000); // Wait before retrying
                        continue;
                    }
                    throw new RemoteException("Server is at maximum capacity. Please try again later.");
                }
                throw e;
            }
        }

        // Clean up previous callback if exists
        if (callback != null) {
            try {
                if (callbackExported) {
                    UnicastRemoteObject.unexportObject(callback, true);
                }
            } catch (Exception e) {
                System.err.println("Error unexporting previous callback: " + e.getMessage());
            }
            callback = null;
            callbackExported = false;
        }
        
        // First load the callback interface to ensure proper typing
        final Class<?> callbackInterface = RMIClassLoader.loadClass(
            System.getProperty("java.rmi.server.codebase"), 
            "SudokuCallback");
        
        // Create callback implementation
        Object callbackImpl = Proxy.newProxyInstance(
            callbackInterface.getClassLoader(),
            new Class<?>[] {callbackInterface},
            new InvocationHandler() {
                public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                    if ("notifyError".equals(method.getName())) {
                        final String message = (String)args[0];
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        return null;
                    } else if ("notifyCompletion".equals(method.getName())) {
                        if (isCompletionDialogShown) return null;
                        isCompletionDialogShown = true;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                int choice = JOptionPane.showConfirmDialog(frame, 
                                    "Congratulations! You solved the puzzle!\nDo you want to play again with a different Sudoku?", 
                                    "Game Over", JOptionPane.YES_NO_OPTION);
                                if (choice == JOptionPane.YES_OPTION) {
                                    new SwingWorker<Void, Void>() {
                                        protected Void doInBackground() throws Exception {
                                            resetGameWithNewPuzzle();
                                            return null;
                                        }
                                        protected void done() {
                                            isCompletionDialogShown = false;
                                        }
                                    }.execute();
                                } else {
                                    try {
                                        unexportCallback();
                                    } catch (Exception e) {
                                        System.err.println("Error during exit: " + e.getMessage());
                                    }
                                    System.exit(0);
                                }
                            }
                        });
                        return null;
                    }
                    throw new UnsupportedOperationException();
                }
            });
        
        // Export callback object
        this.callback = UnicastRemoteObject.exportObject((Remote) callbackImpl, 0);
        callbackExported = true;
        
        // Load game interface
        Class<?> gameInterface = RMIClassLoader.loadClass(
            System.getProperty("java.rmi.server.codebase"), 
            "SudokuInterface");
        
        // Register callback with the exact interface type
        try {
            Method registerMethod = gameInterface.getMethod("registerCallback", callbackInterface);
            registerMethod.invoke(game, callbackImpl);
        } catch (Exception e) {
            unexportCallback();
            throw e;
        }
        
        // Get puzzle and initialize GUI
        String[] puzzle = (String[]) gameInterface.getMethod("getPuzzle").invoke(game);
        initializeGUI(puzzle);
    } catch (Exception e) {
        // Clean up if initialization fails
        cleanup();
        throw e;
    }
}

    private void unexportCallback() {
        if (callback != null && callbackExported) {
            try {
                // Only unexport if the object is actually exported
                if (UnicastRemoteObject.unexportObject(callback, true)) {
                    callbackExported = false;
                }
            } catch (Exception e) {
                System.err.println("Error unexporting callback: " + e.getMessage());
            }
        }
        callback = null;
    }
   
    private void resetGameWithNewPuzzle() {
        try {
            // Clean up current game state
            cleanup();
            
            // Reset UI state
            errors = 0;
            if (textLabel != null) {
                textLabel.setText("Sudoku: " + errors);
            }
            
            // Initialize a new game
            initializeGame();
            
            // Refresh the UI
            if (boardPanel != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        boardPanel.revalidate();
                        boardPanel.repaint();
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, 
                "Error resetting game: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
       public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java SudokuClient <server-ip>");
            System.exit(1);
        }
        final String serverIP = args[0];
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SudokuClient(serverIP);
            }
        });
    }
}