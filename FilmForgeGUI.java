import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class FilmForgeGUI extends JFrame {
    private Process cppProcess;
    private BufferedWriter toBackend;
    private BufferedReader fromBackend;
    private String currentUser = null;
    
    // GUI Components
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    public FilmForgeGUI() {
        setTitle("FILMFORGE - Movie Streaming Platform");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Initialize C++ Backend Connection
        if(!initBackend()) {
            return;
        }
        
        // Setup GUI
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRegisterPanel(), "REGISTER");
        mainPanel.add(createDashboardPanel(), "DASHBOARD");
        
        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
        
        setVisible(true);
        
        // Cleanup on exit
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }
    
    private boolean initBackend() {
        try {
            // Try Windows executable first
            File exeFile = new File("filmforge.exe");
            File unixFile = new File("filmforge");
            
            ProcessBuilder pb;
            if(exeFile.exists()) {
                pb = new ProcessBuilder("filmforge.exe");
            } else if(unixFile.exists()) {
                pb = new ProcessBuilder("./filmforge");
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Error: Backend executable not found!\n\n" +
                    "Please compile the C++ code first:\n" +
                    "Windows: g++ main.cpp -o filmforge.exe\n" +
                    "Linux/Mac: g++ main.cpp -o filmforge\n\n" +
                    "Make sure the executable is in the same folder as this Java file.",
                    "Backend Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return false;
            }
            
            pb.redirectErrorStream(true);
            cppProcess = pb.start();
            
            toBackend = new BufferedWriter(new OutputStreamWriter(cppProcess.getOutputStream()));
            fromBackend = new BufferedReader(new InputStreamReader(cppProcess.getInputStream()));
            
            // Test connection
            Thread.sleep(500); // Give backend time to start
            
            System.out.println("Backend connected successfully!");
            return true;
            
        } catch(IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Error: Cannot start backend process.\n" +
                "Error: " + e.getMessage(),
                "Backend Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
            return false;
        } catch(InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private String sendCommand(String command) {
        try {
            System.out.println("Sending: " + command); // Debug
            toBackend.write(command + "\n");
            toBackend.flush();
            
            String response = fromBackend.readLine();
            System.out.println("Received: " + response); // Debug
            
            return response;
        } catch(IOException e) {
            System.err.println("Communication error: " + e.getMessage());
            e.printStackTrace();
            return "ERROR:Communication error - " + e.getMessage();
        }
    }
    
    // =================== LOGIN PANEL ===================
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title
        JLabel titleLabel = new JLabel("FILMFORGE");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(new Color(229, 9, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Username
        gbc.gridwidth = 1; gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(userLabel, gbc);
        
        gbc.gridx = 1;
        JTextField userField = new JTextField(20);
        userField.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(userField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(passLabel, gbc);
        
        gbc.gridx = 1;
        JPasswordField passField = new JPasswordField(20);
        passField.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(passField, gbc);
        
        // Status label for feedback
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(statusLabel, gbc);
        
        // Login Button
        gbc.gridy = 4;
        JButton loginBtn = new JButton("LOGIN");
        loginBtn.setBackground(new Color(229, 9, 20));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 16));
        loginBtn.setFocusPainted(false);
        loginBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            
            if(username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please enter both username and password!");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            statusLabel.setText("Logging in...");
            statusLabel.setForeground(Color.YELLOW);
            
            String response = sendCommand("LOGIN " + username + " " + password);
            
            if(response != null && response.startsWith("SUCCESS")) {
                currentUser = username;
                statusLabel.setText("Success! Loading dashboard...");
                statusLabel.setForeground(Color.GREEN);
                
                // Clear fields
                userField.setText("");
                passField.setText("");
                
                // Update dashboard and switch immediately
                SwingUtilities.invokeLater(() -> {
                    loadDashboard();
                    cardLayout.show(mainPanel, "DASHBOARD");
                });
            } else {
                statusLabel.setText("Invalid username or password!");
                statusLabel.setForeground(Color.RED);
                System.err.println("Login failed. Response: " + response);
            }
        });
        panel.add(loginBtn, gbc);
        
        // Register Link
        gbc.gridy = 5;
        JButton registerLink = new JButton("New User? Register Here");
        registerLink.setForeground(new Color(100, 149, 237));
        registerLink.setContentAreaFilled(false);
        registerLink.setBorderPainted(false);
        registerLink.setFont(new Font("Arial", Font.PLAIN, 12));
        registerLink.addActionListener(e -> {
            statusLabel.setText(" ");
            cardLayout.show(mainPanel, "REGISTER");
        });
        panel.add(registerLink, gbc);
        
        // Add Enter key support
        ActionListener loginAction = e -> loginBtn.doClick();
        userField.addActionListener(loginAction);
        passField.addActionListener(loginAction);
        
        return panel;
    }
    
    // =================== REGISTER PANEL ===================
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(new Color(229, 9, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Username
        gbc.gridwidth = 1; gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(userLabel, gbc);
        
        gbc.gridx = 1;
        JTextField userField = new JTextField(20);
        userField.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(userField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(passLabel, gbc);
        
        gbc.gridx = 1;
        JPasswordField passField = new JPasswordField(20);
        passField.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(passField, gbc);
        
        // Status label
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(statusLabel, gbc);
        
        // Register Button
        gbc.gridy = 4;
        JButton registerBtn = new JButton("REGISTER");
        registerBtn.setBackground(new Color(229, 9, 20));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFont(new Font("Arial", Font.BOLD, 16));
        registerBtn.setFocusPainted(false);
        registerBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            
            if(username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please fill all fields!");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            if(username.contains(" ") || password.contains(" ")) {
                statusLabel.setText("Username and password cannot contain spaces!");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            statusLabel.setText("Registering...");
            statusLabel.setForeground(Color.YELLOW);
            
            String response = sendCommand("REGISTER " + username + " " + password);
            
            if(response != null && response.startsWith("SUCCESS")) {
                statusLabel.setText("Registration successful!");
                statusLabel.setForeground(Color.GREEN);
                
                JOptionPane.showMessageDialog(this, 
                    "Registration successful!\nYou can now login with your credentials.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                userField.setText("");
                passField.setText("");
                statusLabel.setText(" ");
                cardLayout.show(mainPanel, "LOGIN");
            } else {
                statusLabel.setText("Username already exists!");
                statusLabel.setForeground(Color.RED);
            }
        });
        panel.add(registerBtn, gbc);
        
        // Back to Login
        gbc.gridy = 5;
        JButton backBtn = new JButton("Back to Login");
        backBtn.setForeground(new Color(100, 149, 237));
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        backBtn.addActionListener(e -> {
            statusLabel.setText(" ");
            userField.setText("");
            passField.setText("");
            cardLayout.show(mainPanel, "LOGIN");
        });
        panel.add(backBtn, gbc);
        
        return panel;
    }
    
    // =================== DASHBOARD PANEL ===================
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 20, 20));
        
        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(229, 9, 20));
        topBar.setPreferredSize(new Dimension(0, 60));
        
        JLabel logo = new JLabel("  FILMFORGE");
        logo.setFont(new Font("Arial", Font.BOLD, 24));
        logo.setForeground(Color.WHITE);
        topBar.add(logo, BorderLayout.WEST);
        
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        userPanel.setBackground(new Color(229, 9, 20));
        JLabel userLabel = new JLabel("Welcome, " + (currentUser != null ? currentUser : "User") + "  ");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        userPanel.add(userLabel);
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(Color.WHITE);
        logoutBtn.setForeground(new Color(229, 9, 20));
        logoutBtn.addActionListener(e -> {
            currentUser = null;
            cardLayout.show(mainPanel, "LOGIN");
        });
        userPanel.add(logoutBtn);
        topBar.add(userPanel, BorderLayout.EAST);
        
        panel.add(topBar, BorderLayout.NORTH);
        
        // Main Content Area
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(20, 20, 20));
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        tabbedPane.addTab("All Movies", createAllMoviesPanel());
        tabbedPane.addTab("Trending", createTrendingPanel());
        tabbedPane.addTab("Search", createSearchPanel());
        tabbedPane.addTab("My History", createHistoryPanel());
        tabbedPane.addTab("Recommendations", createRecommendationsPanel());
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadDashboard() {
        try {
            // Remove old dashboard if exists
            Component[] components = mainPanel.getComponents();
            for (int i = 0; i < components.length; i++) {
                if (i == 2) {  // Dashboard is at index 2
                    mainPanel.remove(i);
                    break;
                }
            }
            
            // Add fresh dashboard
            JPanel newDashboard = createDashboardPanel();
            mainPanel.add(newDashboard, "DASHBOARD");
            
            // Force update
            mainPanel.revalidate();
            mainPanel.repaint();
            
            System.out.println("Dashboard loaded for user: " + currentUser);
        } catch(Exception e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private JPanel createAllMoviesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 20, 20));
        
        JPanel moviesPanel = new JPanel(new GridLayout(0, 2, 15, 15));
        moviesPanel.setBackground(new Color(20, 20, 20));
        
        String response = sendCommand("ALLMOIVES");
        if(response != null && response.startsWith("ALLMOVIES:")) {
            String data = response.substring(10);
            String[] movies = data.split("\\|");
            
            for(String movie : movies) {
                String[] parts = movie.split(",");
                if(parts.length >= 5) {
                    moviesPanel.add(createMovieCard(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(moviesPanel);
        scrollPane.getViewport().setBackground(new Color(20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createMovieCard(String id, String title, String genre, String year, String rating) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(new Color(40, 40, 40));
        card.setBorder(BorderFactory.createLineBorder(new Color(229, 9, 20), 2));
        card.setPreferredSize(new Dimension(400, 150));
        
        JPanel infoPanel = new JPanel(new GridLayout(4, 1));
        infoPanel.setBackground(new Color(40, 40, 40));
        
        JLabel titleLabel = new JLabel(" " + title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel genreLabel = new JLabel(" Genre: " + genre);
        genreLabel.setForeground(Color.LIGHT_GRAY);
        
        JLabel yearLabel = new JLabel(" Year: " + year);
        yearLabel.setForeground(Color.LIGHT_GRAY);
        
        JLabel ratingLabel = new JLabel(" Rating: " + rating + " ⭐");
        ratingLabel.setForeground(new Color(255, 215, 0));
        
        infoPanel.add(titleLabel);
        infoPanel.add(genreLabel);
        infoPanel.add(yearLabel);
        infoPanel.add(ratingLabel);
        
        card.add(infoPanel, BorderLayout.CENTER);
        
        JButton watchBtn = new JButton("Watch Now");
        watchBtn.setBackground(new Color(229, 9, 20));
        watchBtn.setForeground(Color.WHITE);
        watchBtn.addActionListener(e -> {
            sendCommand("ADDHISTORY " + currentUser + " " + id);
            JOptionPane.showMessageDialog(this, "Now playing: " + title, "Playing", JOptionPane.INFORMATION_MESSAGE);
        });
        card.add(watchBtn, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JPanel createTrendingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 20, 20));
        
        JLabel header = new JLabel("  🔥 Trending Now");
        header.setFont(new Font("Arial", Font.BOLD, 24));
        header.setForeground(new Color(229, 9, 20));
        panel.add(header, BorderLayout.NORTH);
        
        JPanel trendingPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        trendingPanel.setBackground(new Color(20, 20, 20));
        
        String response = sendCommand("TRENDING 5");
        if(response != null && response.startsWith("TRENDING:")) {
            String data = response.substring(9);
            String[] movies = data.split("\\|");
            
            for(int i = 0; i < movies.length; i++) {
                String[] parts = movies[i].split(",");
                if(parts.length >= 3) {
                    JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    item.setBackground(new Color(40, 40, 40));
                    item.setBorder(BorderFactory.createLineBorder(new Color(229, 9, 20), 2));
                    
                    JLabel rank = new JLabel(" #" + (i+1) + " ");
                    rank.setFont(new Font("Arial", Font.BOLD, 20));
                    rank.setForeground(new Color(255, 215, 0));
                    
                    JLabel titleLabel = new JLabel(parts[1] + " - " + parts[2] + " views");
                    titleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
                    titleLabel.setForeground(Color.WHITE);
                    
                    item.add(rank);
                    item.add(titleLabel);
                    trendingPanel.add(item);
                }
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(trendingPanel);
        scrollPane.getViewport().setBackground(new Color(20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(20, 20, 20));
        
        // Search Bar
        JPanel searchBar = new JPanel(new FlowLayout());
        searchBar.setBackground(new Color(20, 20, 20));
        
        JLabel searchLabel = new JLabel("Search by ID:");
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        
        JTextField searchField = new JTextField(15);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(new Color(229, 9, 20));
        searchBtn.setForeground(Color.WHITE);
        
        JPanel resultPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        resultPanel.setBackground(new Color(20, 20, 20));
        
        searchBtn.addActionListener(e -> {
            resultPanel.removeAll();
            String id = searchField.getText();
            String response = sendCommand("SEARCH " + id);
            
            if(response != null && response.startsWith("MOVIE:")) {
                String data = response.substring(6);
                String[] parts = data.split("\\|");
                if(parts.length >= 6) {
                    resultPanel.add(createMovieCard(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            } else {
                JLabel errorLabel = new JLabel("Movie not found!");
                errorLabel.setForeground(Color.RED);
                errorLabel.setFont(new Font("Arial", Font.BOLD, 16));
                resultPanel.add(errorLabel);
            }
            resultPanel.revalidate();
            resultPanel.repaint();
        });
        
        searchBar.add(searchLabel);
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        
        panel.add(searchBar, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(resultPanel);
        scrollPane.getViewport().setBackground(new Color(20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Autocomplete Section
        JPanel autocompletePanel = new JPanel(new FlowLayout());
        autocompletePanel.setBackground(new Color(20, 20, 20));
        
        JLabel autoLabel = new JLabel("Autocomplete:");
        autoLabel.setForeground(Color.WHITE);
        autoLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        
        JTextField autoField = new JTextField(15);
        autoField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JTextArea suggestionsArea = new JTextArea(5, 30);
        suggestionsArea.setEditable(false);
        suggestionsArea.setBackground(new Color(40, 40, 40));
        suggestionsArea.setForeground(Color.WHITE);
        
        autoField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String prefix = autoField.getText();
                if(prefix.length() > 0) {
                    String response = sendCommand("AUTOCOMPLETE " + prefix);
                    if(response != null && response.startsWith("SUGGESTIONS:")) {
                        String suggestions = response.substring(12).replace("|", "\n");
                        suggestionsArea.setText(suggestions);
                    }
                } else {
                    suggestionsArea.setText("");
                }
            }
        });
        
        autocompletePanel.add(autoLabel);
        autocompletePanel.add(autoField);
        panel.add(autocompletePanel, BorderLayout.SOUTH);
        panel.add(new JScrollPane(suggestionsArea), BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 20, 20));
        
        JLabel header = new JLabel("  📺 Watch History");
        header.setFont(new Font("Arial", Font.BOLD, 24));
        header.setForeground(new Color(229, 9, 20));
        panel.add(header, BorderLayout.NORTH);
        
        JPanel historyPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        historyPanel.setBackground(new Color(20, 20, 20));
        
        String response = sendCommand("GETHISTORY " + currentUser);
        if(response != null && response.startsWith("HISTORY:")) {
            String data = response.substring(8);
            if(!data.isEmpty()) {
                String[] movieIds = data.split("\\|");
                
                for(String id : movieIds) {
                    String movieResponse = sendCommand("SEARCH " + id);
                    if(movieResponse != null && movieResponse.startsWith("MOVIE:")) {
                        String movieData = movieResponse.substring(6);
                        String[] parts = movieData.split("\\|");
                        if(parts.length >= 5) {
                            JLabel item = new JLabel("  " + parts[1] + " (" + parts[2] + ")");
                            item.setFont(new Font("Arial", Font.PLAIN, 16));
                            item.setForeground(Color.WHITE);
                            item.setOpaque(true);
                            item.setBackground(new Color(40, 40, 40));
                            item.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));
                            historyPanel.add(item);
                        }
                    }
                }
            } else {
                JLabel emptyLabel = new JLabel("No watch history yet!");
                emptyLabel.setForeground(Color.LIGHT_GRAY);
                emptyLabel.setFont(new Font("Arial", Font.ITALIC, 16));
                historyPanel.add(emptyLabel);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(historyPanel);
        scrollPane.getViewport().setBackground(new Color(20, 20, 20));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRecommendationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(20, 20, 20));
        
        JLabel header = new JLabel("  🎬 Discover by Genre");
        header.setFont(new Font("Arial", Font.BOLD, 24));
        header.setForeground(new Color(229, 9, 20));
        panel.add(header, BorderLayout.NORTH);
        
        JPanel genrePanel = new JPanel(new FlowLayout());
        genrePanel.setBackground(new Color(20, 20, 20));
        
        String[] genres = {"Action", "Sci-Fi", "Comedy", "Horror", "Drama", "Romance", "Thriller"};
        
        JTextArea resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(40, 40, 40));
        resultArea.setForeground(Color.WHITE);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 14));
        
        for(String genre : genres) {
            JButton genreBtn = new JButton(genre);
            genreBtn.setBackground(new Color(229, 9, 20));
            genreBtn.setForeground(Color.WHITE);
            genreBtn.addActionListener(e -> {
                String response = sendCommand("RECOMMEND " + genre);
                if(response != null && response.startsWith("RELATED:")) {
                    String related = response.substring(8);
                    resultArea.setText("If you like " + genre + ", try:\n" + related.replace("|", "\n"));
                }
            });
            genrePanel.add(genreBtn);
        }
        
        panel.add(genrePanel, BorderLayout.CENTER);
        panel.add(new JScrollPane(resultArea), BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void cleanup() {
        try {
            if(toBackend != null) {
                toBackend.write("EXIT\n");
                toBackend.flush();
                toBackend.close();
            }
            if(fromBackend != null) fromBackend.close();
            if(cppProcess != null) cppProcess.destroy();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FilmForgeGUI());
    }
}