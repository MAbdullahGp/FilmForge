import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.net.URL;

// ==========================================
//  1. C++ BACKEND BRIDGE
// ==========================================
class CppBackend {
    private List<String> runCpp(String... args) {
        List<String> output = new ArrayList<>();
        try {
            List<String> command = new ArrayList<>();
            command.add("filmforge.exe"); // Make sure this matches your exe name
            for(String arg : args) command.add(arg);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) output.add(line);
            p.waitFor();
        } catch (Exception e) { 
            System.out.println("Backend Error: filmforge.exe missing.");
        }
        return output;
    }

    private List<MovieData> parse(List<String> lines) {
        List<MovieData> list = new ArrayList<>();
        for (String line : lines) {
            if(line.startsWith("TOP_GENRE:") || line.equals("SUCCESS") || line.equals("FAIL")) continue;
            try {
                String[] p = line.split(" ");
                if (p.length >= 5) {
                    list.add(new MovieData(Integer.parseInt(p[0]), p[1], p[2], p[3], Integer.parseInt(p[4])));
                }
            } catch(Exception e) {}
        }
        return list;
    }

    public boolean login(String u, String p) {
        List<String> res = runCpp("login", u, p);
        return !res.isEmpty() && res.get(0).trim().equals("SUCCESS");
    }
    public boolean register(String u, String p) {
        List<String> res = runCpp("register", u, p);
        return !res.isEmpty() && res.get(0).trim().equals("SUCCESS");
    }
    public List<MovieData> getAllMovies() { return parse(runCpp("load_all")); }
    public List<MovieData> getTrending() { return parse(runCpp("trending")); }
    public List<MovieData> getSortedByViews() { return parse(runCpp("sort_views")); }
    public List<MovieData> getSortedByName() { return parse(runCpp("sort_name")); }
    public List<MovieData> searchMovies(String query) { return parse(runCpp("search", query)); }
    public List<MovieData> getHistory(String user) { return parse(runCpp("history", user)); }

    public Map<String, Object> getSuggestions(String user) {
        List<String> raw = runCpp("recommend", user);
        String genre = "None";
        List<MovieData> recs = new ArrayList<>();
        for(String line : raw) {
            if(line.startsWith("TOP_GENRE:")) {
                String[] parts = line.split(":");
                if(parts.length > 1) genre = parts[1];
            } else {
                try {
                    String[] p = line.split(" ");
                    if(p.length >= 5) recs.add(new MovieData(Integer.parseInt(p[0]), p[1], p[2], p[3], Integer.parseInt(p[4])));
                } catch(Exception e) {}
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("genre", genre); res.put("list", recs);
        return res;
    }
}

class MovieData {
    int id; String title; String genre; String path; int views;
    MovieData(int i, String t, String g, String p, int v) {id=i;title=t;genre=g;path=p;views=v;}
}

// ==========================================
//  2. GUI CLASS (NETFLIX PAGING SYSTEM)
// ==========================================
public class FilmForgeGUI extends JFrame {

    private CppBackend backend = new CppBackend();
    private String currentUser = "Guest";

    // --- STYLING ---
    private static final Color BG_DARK = new Color(18, 18, 18);
    private static final Color BG_SIDEBAR = new Color(10, 10, 10);
    private static final Color BG_CARD = new Color(40, 40, 40);
    private static final Color ACCENT_RED = new Color(229, 9, 20); 
    private static final Color HOVER_RED = new Color(180, 0, 0);  
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(180, 180, 180);
    private static final Color TEXT_GRAY = new Color(150, 150, 150);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 32); 
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 22); 
    private static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 16); 
    
    private static final String DUMMY_VIDEO_PATH = "video.mp4"; 

    // Components
    private CardLayout parentCardLayout = new CardLayout();
    private JPanel parentPanel = new JPanel(parentCardLayout);
    private CardLayout contentCardLayout = new CardLayout();
    private JPanel contentPanel = new JPanel(contentCardLayout);
    
    // Main Container
    private JPanel homeContainer = new JPanel();
    private JScrollPane mainScrollPane;

    private JTextField topSearchBar;
    private JComboBox<String> sortDropdown; 
    private JPanel profilePanel; 

    public FilmForgeGUI() {
        setTitle("FilmForge - Ultimate Cinema");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        homeContainer.setLayout(new BoxLayout(homeContainer, BoxLayout.Y_AXIS));
        homeContainer.setBackground(BG_DARK);

        initLoginScreen();
        initIntroScreen();
        initDashboardScreen();

        add(parentPanel);
        parentCardLayout.show(parentPanel, "LOGIN");
        setVisible(true);
    }

    // --- UTILS ---
    private void styleRedButton(JButton btn) {
        btn.setBackground(ACCENT_RED);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(HOVER_RED); }
            public void mouseExited(MouseEvent e) { btn.setBackground(ACCENT_RED); }
        });
    }

    private void styleModernSearchBar(JTextField field) {
        field.setBackground(new Color(45, 45, 45)); 
        field.setForeground(Color.GRAY);
        field.setText("Search Movies...");
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setBorder(new CompoundBorder(new LineBorder(new Color(80, 80, 80), 1, true), new EmptyBorder(8, 15, 8, 15)));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if(field.getText().equals("Search Movies...")) { field.setText(""); field.setForeground(Color.WHITE); }
                field.setBorder(new CompoundBorder(new LineBorder(ACCENT_RED, 2, true), new EmptyBorder(8, 15, 8, 15)));
            }
            public void focusLost(FocusEvent e) {
                if(field.getText().isEmpty()) { field.setText("Search Movies..."); field.setForeground(Color.GRAY); }
                field.setBorder(new CompoundBorder(new LineBorder(new Color(80, 80, 80), 1, true), new EmptyBorder(8, 15, 8, 15)));
            }
        });
    }

    // --- SCREENS ---
    private void initLoginScreen() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(BG_DARK);
        JLabel l = new JLabel("FILM FORGE"); l.setFont(new Font("Segoe UI", Font.BOLD, 50)); l.setForeground(ACCENT_RED);
        JTextField u = new JTextField(20); styleField(u);
        JPasswordField pass = new JPasswordField(20); styleField(pass);
        JButton loginBtn = new JButton("LOGIN"); styleRedButton(loginBtn);
        JButton regBtn = new JButton("REGISTER"); regBtn.setBackground(BG_CARD); regBtn.setForeground(Color.WHITE);

        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10,10,10,10);
        g.gridx=0; g.gridy=0; g.gridwidth=2; p.add(l, g);
        g.gridy=1; p.add(createLabeledField("Username", u), g);
        g.gridy=2; p.add(createLabeledField("Password", pass), g);
        g.gridy=3; g.gridwidth=1; p.add(loginBtn, g);
        g.gridx=1; p.add(regBtn, g);

        loginBtn.addActionListener(e -> {
            if(backend.login(u.getText(), new String(pass.getPassword()))) {
                currentUser = u.getText();
                updateProfileDisplay();
                playIntroAndRedirect();
            } else JOptionPane.showMessageDialog(this, "Invalid Credentials!");
        });

        regBtn.addActionListener(e -> {
             if(backend.register(u.getText(), new String(pass.getPassword()))) JOptionPane.showMessageDialog(this, "Registered!");
             else JOptionPane.showMessageDialog(this, "User Exists!");
        });
        parentPanel.add(p, "LOGIN");
    }

    private void playIntroAndRedirect() {
        parentCardLayout.show(parentPanel, "INTRO");
        javax.swing.Timer t = new javax.swing.Timer(3000, e -> {
            refreshView(backend.getAllMovies(), true); 
            parentCardLayout.show(parentPanel, "DASHBOARD");
        });
        t.setRepeats(false); t.start();
    }

    private void initIntroScreen() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(Color.BLACK);
        ImageIcon icon = new ImageIcon("intro.gif");
        JLabel l = new JLabel(icon); 
        p.add(l, BorderLayout.CENTER); parentPanel.add(p, "INTRO");
    }

    private void initDashboardScreen() {
        JPanel dashboard = new JPanel(new BorderLayout());
        
        // SIDEBAR
        JPanel sidebar = new JPanel(); sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SIDEBAR); sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBorder(new EmptyBorder(30, 15, 30, 15));

        JLabel logo = new JLabel("FILM FORGE"); logo.setFont(new Font("Segoe UI", Font.BOLD, 32)); logo.setForeground(ACCENT_RED);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logo); sidebar.add(Box.createRigidArea(new Dimension(0, 50)));
        
        JButton b1 = sideBtn("All Movies");
        JButton b2 = sideBtn("Trending");
        JButton b3 = sideBtn("History");
        JButton b4 = sideBtn("Suggestions");
        JButton bOut = new JButton("LOGOUT"); styleRedButton(bOut);
        bOut.setMaximumSize(new Dimension(250, 50)); bOut.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(b1); sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(b2); sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(b3); sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(b4); sidebar.add(Box.createVerticalGlue());
        sidebar.add(bOut);

        // TOP BAR
        JPanel top = new JPanel(new BorderLayout()); top.setBackground(BG_DARK);
        top.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(50,50,50)), new EmptyBorder(15,30,15,30)));
        
        JPanel centerP = new JPanel(new FlowLayout(FlowLayout.CENTER)); centerP.setBackground(BG_DARK);
        topSearchBar = new JTextField(35); styleModernSearchBar(topSearchBar);
        JButton searchBtn = new JButton("\u279C"); styleRedButton(searchBtn); searchBtn.setPreferredSize(new Dimension(50, 40));
        
        String[] sorts = {"Sort: Default", "Most Viewed", "Name (A-Z)"};
        sortDropdown = new JComboBox<>(sorts);
        sortDropdown.setBackground(BG_CARD); sortDropdown.setForeground(Color.WHITE);
        
        centerP.add(topSearchBar); centerP.add(searchBtn); centerP.add(sortDropdown);
        profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); profilePanel.setBackground(BG_DARK);
        updateProfileDisplay();

        top.add(centerP, BorderLayout.CENTER); top.add(profilePanel, BorderLayout.EAST);

        // CONTENT
        mainScrollPane = new JScrollPane(homeContainer);
        mainScrollPane.setBorder(null);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contentPanel.add(mainScrollPane, "HOME");

        dashboard.add(sidebar, BorderLayout.WEST);
        dashboard.add(top, BorderLayout.NORTH);
        dashboard.add(contentPanel, BorderLayout.CENTER);

        // Listeners
        b1.addActionListener(e -> refreshView(backend.getAllMovies(), true));
        b2.addActionListener(e -> refreshView(backend.getTrending(), false));
        b3.addActionListener(e -> refreshView(backend.getHistory(currentUser), false));
        b4.addActionListener(e -> loadSuggestions());

        ActionListener searchAction = e -> {
            String q = topSearchBar.getText();
            if(!q.isEmpty() && !q.equals("Search Movies...")) refreshView(backend.searchMovies(q), false);
            else {
                int s = sortDropdown.getSelectedIndex();
                if(s==1) refreshView(backend.getSortedByViews(), false);
                else if(s==2) refreshView(backend.getSortedByName(), false);
                else refreshView(backend.getAllMovies(), true);
            }
        };
        searchBtn.addActionListener(searchAction);
        topSearchBar.addActionListener(searchAction);
        sortDropdown.addActionListener(searchAction);

        bOut.addActionListener(e -> parentCardLayout.show(parentPanel, "LOGIN"));
        parentPanel.add(dashboard, "DASHBOARD");
    }

    // --- DISPLAY LOGIC ---
    private void refreshView(List<MovieData> movies, boolean categorize) {
        homeContainer.removeAll();
        
        if (categorize) {
            Set<String> genres = new LinkedHashSet<>();
            for(MovieData m : movies) genres.add(m.genre);
            
            for(String genre : genres) {
                // FIXED: Manually filter without stream collectors to be safe
                List<MovieData> genreMovies = new ArrayList<>();
                for(MovieData m : movies) {
                    if(m.genre.equals(genre)) genreMovies.add(m);
                }
                
                if(!genreMovies.isEmpty()) {
                    // USE PAGINATED ROW (No Scrollbar)
                    CategoryPagingRow row = new CategoryPagingRow(genre, genreMovies);
                    homeContainer.add(row);
                    homeContainer.add(Box.createVerticalStrut(10));
                }
            }
        } else {
            // GRID VIEW
            JLabel title = new JLabel("  Results");
            title.setFont(FONT_HEADER); title.setForeground(TEXT_PRIMARY);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            homeContainer.add(Box.createVerticalStrut(20)); homeContainer.add(title); homeContainer.add(Box.createVerticalStrut(10));
            
            JPanel grid = new JPanel(new GridLayout(0, 5, 20, 20));
            grid.setBackground(BG_DARK);
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);
            for(MovieData m : movies) grid.add(createVerticalMovieCard(m));
            
            homeContainer.add(grid);
        }
        
        homeContainer.revalidate(); homeContainer.repaint();
    }

    // ========================================================
    //  CUSTOM ROW: HEADER BUTTONS + NO SCROLLBAR (SWAP LOGIC)
    // ========================================================
    class CategoryPagingRow extends JPanel {
        private List<MovieData> movies;
        private int startIndex = 0;
        private int ITEMS_PER_PAGE = 5; // Exactly 5 movies at a time
        private JPanel cardsPanel;
        private JButton nextBtn;
        private JButton prevBtn;

        public CategoryPagingRow(String title, List<MovieData> data) {
            this.movies = data;
            this.setLayout(new BorderLayout());
            this.setBackground(BG_DARK);
            this.setMaximumSize(new Dimension(2000, 310));
            this.setAlignmentX(Component.LEFT_ALIGNMENT);

            // --- 1. HEADER (Title Left, Buttons Right) ---
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(BG_DARK);
            headerPanel.setBorder(new EmptyBorder(0, 5, 0, 30));

            JLabel titleLbl = new JLabel(" " + title);
            titleLbl.setFont(FONT_HEADER);
            titleLbl.setForeground(Color.WHITE);
            headerPanel.add(titleLbl, BorderLayout.WEST);

            // Controls Panel (Buttons)
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            controls.setBackground(BG_DARK);

            prevBtn = createNavButton("<");
            nextBtn = createNavButton(">");

            // BACK CLICK
            prevBtn.addActionListener(e -> {
                if (startIndex > 0) {
                    startIndex -= ITEMS_PER_PAGE;
                    updateCards();
                }
            });

            // NEXT CLICK
            nextBtn.addActionListener(e -> {
                if (startIndex + ITEMS_PER_PAGE < movies.size()) {
                    startIndex += ITEMS_PER_PAGE;
                    updateCards();
                }
            });

            controls.add(prevBtn);
            controls.add(nextBtn);
            headerPanel.add(controls, BorderLayout.EAST);

            this.add(headerPanel, BorderLayout.NORTH);

            // --- 2. MOVIES PANEL (NO SCROLLBAR, JUST GRID) ---
            cardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
            cardsPanel.setBackground(BG_DARK);
            updateCards(); // Initial Load

            this.add(cardsPanel, BorderLayout.CENTER);
        }

        private void updateCards() {
            cardsPanel.removeAll();
            
            int end = Math.min(startIndex + ITEMS_PER_PAGE, movies.size());
            for (int i = startIndex; i < end; i++) {
                cardsPanel.add(createVerticalMovieCard(movies.get(i)));
            }
            
            // Toggle Buttons Visibility
            prevBtn.setVisible(startIndex > 0);
            nextBtn.setVisible(startIndex + ITEMS_PER_PAGE < movies.size());
            
            cardsPanel.revalidate();
            cardsPanel.repaint();
        }

        private JButton createNavButton(String text) {
            JButton btn = new JButton(text);
            btn.setBackground(new Color(40, 40, 40)); 
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(40, 30));
            return btn;
        }
    }

    private void loadSuggestions() {
        homeContainer.removeAll();
        Map<String, Object> data = backend.getSuggestions(currentUser);
        String genre = (String) data.get("genre");
        List<MovieData> list = (List<MovieData>) data.get("list");
        
        JLabel l = new JLabel("  Recommended (Because you watch " + genre + ")");
        l.setFont(FONT_HEADER); l.setForeground(Color.WHITE); l.setAlignmentX(Component.LEFT_ALIGNMENT);
        homeContainer.add(Box.createVerticalStrut(20)); homeContainer.add(l); homeContainer.add(Box.createVerticalStrut(10));
        
        JPanel grid = new JPanel(new GridLayout(0, 5, 20, 20));
        grid.setBackground(BG_DARK); grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for(MovieData m : list) grid.add(createVerticalMovieCard(m));
        
        homeContainer.add(grid);
        homeContainer.revalidate(); homeContainer.repaint();
    }

    private JPanel createVerticalMovieCard(MovieData m) {
        JPanel card = new JPanel(); card.setPreferredSize(new Dimension(180, 260));
        card.setBackground(BG_CARD); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createLineBorder(new Color(60,60,60), 1));
        
        JLabel img = new JLabel("Loading...", SwingConstants.CENTER);
        img.setPreferredSize(new Dimension(180, 140)); img.setForeground(Color.GRAY);
        img.setMaximumSize(new Dimension(180, 140)); img.setBackground(Color.BLACK); img.setOpaque(true);
        
        new Thread(() -> {
            try {
                // Uses Title Hash for unique images
                int uniqueLock = Math.abs(m.title.hashCode());
                URL url = new URL("https://loremflickr.com/300/450/movie,cinema?lock=" + uniqueLock);
                BufferedImage rawImage = ImageIO.read(url);
                if(rawImage != null) {
                    Image scaledImage = rawImage.getScaledInstance(180, 140, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaledImage);
                    SwingUtilities.invokeLater(() -> {
                        img.setIcon(icon);
                        img.setText("");
                    });
                }
            } catch(Exception e){
                 SwingUtilities.invokeLater(() -> img.setText("Error"));
            }
        }).start();

        JPanel info = new JPanel(); info.setBackground(BG_CARD); info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(10,10,10,10));
        
        JLabel t = new JLabel(m.title.replace("_", " ")); t.setForeground(TEXT_PRIMARY); t.setFont(FONT_CARD_TITLE);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel v = new JLabel(m.views + " views"); v.setForeground(TEXT_SECONDARY); v.setFont(FONT_REGULAR);
        v.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton w = new JButton("WATCH"); styleRedButton(w); w.setAlignmentX(Component.LEFT_ALIGNMENT);
        w.addActionListener(e -> { recordHistory(m.id); playVideo(); });

        info.add(t); info.add(Box.createVerticalStrut(5)); info.add(v); info.add(Box.createVerticalStrut(10)); info.add(w);
        card.add(img); card.add(info);
        return card;
    }

    private void recordHistory(int id) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("history.txt", true))){
            bw.write(currentUser+" "+id); bw.newLine();
        }catch(Exception e){}
    }

    private void updateProfileDisplay() {
        if(profilePanel == null) return;
        profilePanel.removeAll();
        JLabel l = new JLabel("Hi, " + currentUser); l.setForeground(TEXT_PRIMARY); l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        profilePanel.add(l); profilePanel.revalidate(); profilePanel.repaint();
    }

    private JButton sideBtn(String t) {
        JButton b = new JButton(t); b.setMaximumSize(new Dimension(250, 50));
        b.setBackground(ACCENT_RED); b.setForeground(Color.WHITE); 
        b.setFont(new Font("Segoe UI", Font.BOLD, 16)); b.setAlignmentX(Component.CENTER_ALIGNMENT);
        styleRedButton(b);
        return b;
    }
    private void styleField(JTextField f) {
        f.setBackground(new Color(60,60,60)); f.setForeground(Color.WHITE); f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
    }
    private JPanel createLabeledField(String l, JTextField f) {
        JPanel p=new JPanel(new BorderLayout()); p.setBackground(BG_DARK);
        JLabel lbl=new JLabel(l); lbl.setForeground(TEXT_SECONDARY);
        p.add(lbl, BorderLayout.NORTH); p.add(f, BorderLayout.CENTER); return p;
    }
    private void playVideo() {
        try { Desktop.getDesktop().open(new File(DUMMY_VIDEO_PATH)); }
        catch(Exception e) { JOptionPane.showMessageDialog(this, "Error !Video File Missing!"); }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new FilmForgeGUI()); }
}