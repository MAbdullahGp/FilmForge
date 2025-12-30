import javax.swing.*;
import java.awt.*; 
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*; 
import java.util.List; 
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

// ==========================================
// PART 1: DATA STRUCTURES
// ==========================================

class Movie {
    String title;
    String category;
    double rating;
    String imageUrl;

    public Movie(String t, String c, double r, String u) {
        this.title = t;
        this.category = c;
        this.rating = r;
        this.imageUrl = u;
    }
}

class AnalyticsEngine {
    public Map<String, Integer> userPreferences = new HashMap<>();

    public AnalyticsEngine() {
        userPreferences.put("Hollywood", 0);
        userPreferences.put("Bollywood", 0);
        userPreferences.put("Korean", 0);
        userPreferences.put("Tollywood", 0);
    }

    public void logAction(String category) {
        userPreferences.put(category, userPreferences.getOrDefault(category, 0) + 1);
    }
    
    public String getTopCategory() {
        String topCat = "Hollywood";
        int max = -1;
        for(Map.Entry<String, Integer> e : userPreferences.entrySet()) {
            if(e.getValue() > max) {
                max = e.getValue();
                topCat = e.getKey();
            }
        }
        return topCat;
    }

    public int getTotalInteractions() {
        int total = 0;
        for (int count : userPreferences.values()) total += count;
        return total;
    }
}

class ImageLoader {
    private static Map<String, ImageIcon> cache = new HashMap<>();
    private static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void load(String originalUrl, String movieTitle, JLabel targetLabel) {
        if (cache.containsKey(originalUrl)) {
            targetLabel.setIcon(cache.get(originalUrl));
            targetLabel.setText("");
            return;
        }

        executor.submit(() -> {
            ImageIcon icon = null;
            try {
                String proxyUrl = "https://wsrv.nl/?url=" + originalUrl + "&w=140&h=200&output=jpg";
                URL url = new URL(proxyUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(8000); 
                conn.setReadTimeout(8000);
                InputStream is = conn.getInputStream();
                Image img = ImageIO.read(is);
                if (img != null) {
                    Image scaled = img.getScaledInstance(140, 200, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaled);
                }
            } catch (Exception e) { }

            final ImageIcon finalIcon = icon;
            SwingUtilities.invokeLater(() -> {
                if (finalIcon != null) {
                    cache.put(originalUrl, finalIcon);
                    targetLabel.setIcon(finalIcon);
                } else {
                    targetLabel.setIcon(createTitlePlaceholder(movieTitle));
                }
                targetLabel.setText("");
            });
        });
    }

    private static ImageIcon createTitlePlaceholder(String title) {
        int w = 140; int h = 200;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(229, 9, 20)); 
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(0, 0, w, h);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int y = h / 2 - 10;
        String[] words = title.split(" ");
        for (String word : words) {
            int x = (w - fm.stringWidth(word)) / 2;
            g2.drawString(word, x, y);
            y += 20;
        }
        g2.dispose();
        return new ImageIcon(img);
    }
}

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord = false;
    String movieTitle;
}

class Trie {
    private TrieNode root = new TrieNode();
    public void insert(String title) {
        TrieNode curr = root;
        for (char c : title.toLowerCase().toCharArray()) {
            curr.children.putIfAbsent(c, new TrieNode());
            curr = curr.children.get(c);
        }
        curr.isEndOfWord = true;
        curr.movieTitle = title;
    }
    public List<String> search(String pre) {
        List<String> res = new ArrayList<>();
        TrieNode curr = root;
        for (char c : pre.toLowerCase().toCharArray()) {
            if (!curr.children.containsKey(c)) return res;
            curr = curr.children.get(c);
        }
        dfs(curr, res);
        return res;
    }
    private void dfs(TrieNode n, List<String> r) {
        if (n.isEndOfWord) r.add(n.movieTitle);
        for (TrieNode c : n.children.values()) dfs(c, r);
    }
}

class Graph {
    Map<String, List<Movie>> adjList = new HashMap<>();
    public void addMovie(Movie m) {
        adjList.putIfAbsent(m.category, new ArrayList<>());
        adjList.get(m.category).add(m);
    }
    public List<Movie> getMovies(String cat) { return adjList.getOrDefault(cat, new ArrayList<>()); }
    public Movie findMovie(String t) {
        for(List<Movie> l : adjList.values()) for(Movie m : l) if(m.title.equalsIgnoreCase(t)) return m;
        return null;
    }
}

// ==========================================
// PHYSICS GRAPH NODE
// ==========================================
class GraphNode {
    double x, y;
    double vx, vy; 
    String label;
    boolean isCategory;
    Movie movieData; 
    Color nodeColor; 

    public GraphNode(double x, double y, String label, boolean isCategory, Movie m, Color c) {
        this.x = x; this.y = y;
        this.label = label;
        this.isCategory = isCategory;
        this.movieData = m;
        this.nodeColor = c;
    }
}

// ==========================================
// MAIN APPLICATION
// ==========================================

public class FilmForgeApp {
    private Trie trie = new Trie();
    private Graph graph = new Graph();
    private AnalyticsEngine analytics = new AnalyticsEngine();
    
    private JFrame frame;
    private JPanel watchlistPanel;
    private List<Movie> myWatchList = new ArrayList<>();
    private JPanel historyPanel;
    private List<Movie> myHistory = new ArrayList<>();
    
    private AdvancedGraphPanel advancedGraphPanel;
    private JPanel recListPanel;
    private BarChartPanel barChartPanel; 

    Color BG = new Color(18, 18, 18);
    Color ITEM_BG = new Color(40, 40, 40);
    // HERE IS THE FIX: Added ACCENT back
    Color ACCENT = new Color(229, 9, 20); 
    
    // Category Colors
    Color COLOR_HOLLYWOOD = new Color(229, 9, 20); 
    Color COLOR_BOLLYWOOD = new Color(66, 133, 244); 
    Color COLOR_KOREAN = new Color(255, 193, 7); 
    Color COLOR_TOLLYWOOD = new Color(76, 175, 80); 

    public FilmForgeApp() {
        loadData(); 

        frame = new JFrame("FilmForge - Advanced DSA Movie App");
        frame.setSize(1280, 850);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BG);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(Color.WHITE);

        tabs.addTab("Home", createHomeTab());
        tabs.addTab("Recommendations (AI Graph)", createRecommendationTab());
        tabs.addTab("Search", createSearchTab());
        tabs.addTab("My Watchlist", createWatchTab());
        tabs.addTab("History", createHistoryTab());

        tabs.addChangeListener(e -> {
            if(tabs.getSelectedIndex() == 1) { 
                updateRecommendationView();
            }
        });

        frame.add(tabs, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private Color getCategoryColor(String cat) {
        switch(cat) {
            case "Hollywood": return COLOR_HOLLYWOOD;
            case "Bollywood": return COLOR_BOLLYWOOD;
            case "Korean": return COLOR_KOREAN;
            case "Tollywood": return COLOR_TOLLYWOOD;
            default: return Color.GRAY;
        }
    }

    private void playMovie(Movie m) {
        analytics.logAction(m.category);
        myHistory.remove(m); 
        myHistory.add(0, m); 
        refreshHistory(); 
        updateRecommendationView(); 

        JOptionPane.showMessageDialog(frame, "Starting: " + m.title + "\n(Playing Dummy Video...)", "Now Playing", JOptionPane.INFORMATION_MESSAGE);
        try {
            File videoFile = new File("dummy.mp4"); 
            if (!videoFile.exists()) videoFile = new File("../dummy.mp4");
            if (videoFile.exists()) Desktop.getDesktop().open(videoFile);
            else JOptionPane.showMessageDialog(frame, "dummy.mp4 not found!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private JScrollPane createHomeTab() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG);

        String[] cats = {"Hollywood", "Bollywood", "Korean", "Tollywood"};

        for (String c : cats) {
            JLabel title = new JLabel("  " + c);
            title.setForeground(getCategoryColor(c)); 
            title.setFont(new Font("Arial", Font.BOLD, 22));
            title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JPanel rowPanel = new JPanel(new GridLayout(2, 5, 15, 15)); 
            rowPanel.setBackground(BG);
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 20, 15)); 

            List<Movie> movies = graph.getMovies(c);
            for(Movie m : movies) {
                addCard(rowPanel, m, "ADD");
            }

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(BG);
            wrapper.add(title, BorderLayout.NORTH);
            wrapper.add(rowPanel, BorderLayout.CENTER);
            mainPanel.add(wrapper);
        }

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBorder(null);
        return scroll;
    }

    private JPanel createRecommendationTab() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);
        
        JPanel topSplitPanel = new JPanel(new GridBagLayout()); 
        topSplitPanel.setBackground(BG);
        topSplitPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();

        advancedGraphPanel = new AdvancedGraphPanel();
        advancedGraphPanel.setPreferredSize(new Dimension(850, 450)); 
        advancedGraphPanel.setBackground(new Color(10, 10, 20));
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.7; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 15);
        topSplitPanel.add(advancedGraphPanel, gbc);

        barChartPanel = new BarChartPanel();
        barChartPanel.setPreferredSize(new Dimension(350, 450));
        barChartPanel.setBackground(new Color(25, 25, 25));

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 0.3; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        topSplitPanel.add(barChartPanel, gbc);

        recListPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        recListPanel.setBackground(BG);
        JScrollPane scrollList = new JScrollPane(recListPanel);
        scrollList.setBorder(null);
        scrollList.setPreferredSize(new Dimension(1200, 280));

        main.add(topSplitPanel, BorderLayout.CENTER);
        main.add(scrollList, BorderLayout.SOUTH);

        return main;
    }

    private void updateRecommendationView() {
        String topCat = analytics.getTopCategory();
        
        advancedGraphPanel.initMixedGraph(analytics, graph);
        advancedGraphPanel.startSimulation();

        List<Movie> recMovies = graph.getMovies(topCat);
        recListPanel.removeAll();
        JLabel header = new JLabel("<html><h2>Top Recommendations (<font color='" + getHexColor(getCategoryColor(topCat)) + "'>" + topCat + "</font>)</h2></html>");
        header.setForeground(Color.WHITE);
        recListPanel.add(header);
        for(Movie m : recMovies) {
            addCard(recListPanel, m, "ADD");
        }
        recListPanel.revalidate();
        recListPanel.repaint();
        
        barChartPanel.repaint();
    }
    
    private String getHexColor(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    class BarChartPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth(); int height = getHeight();
            int padding = 40; int barWidth = 50; 
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("User Interest Statistics (Category %)", 20, 20);

            Map<String, Integer> prefs = analytics.userPreferences;
            int total = analytics.getTotalInteractions();
            if(total == 0) {
                g2.drawString("Start adding movies to see stats!", width/2 - 100, height/2);
                return;
            }

            String[] categories = {"Hollywood", "Bollywood", "Korean", "Tollywood"};
            Color[] colors = {COLOR_HOLLYWOOD, COLOR_BOLLYWOOD, COLOR_KOREAN, COLOR_TOLLYWOOD};

            int startX = padding + 20;
            int maxBarHeight = height - padding - 60;
            int gap = (width - (2 * padding) - (categories.length * barWidth)) / (categories.length - 1);
            if (gap < 5) gap = 5;

            for(int i=0; i<categories.length; i++) {
                String cat = categories[i];
                int value = prefs.getOrDefault(cat, 0);
                int barHeight = (int) ((double)value / total * maxBarHeight);
                int x = startX + (i * (barWidth + gap));
                int y = height - padding - barHeight;

                g2.setColor(colors[i]);
                g2.fillRect(x, y, barWidth, barHeight);
                g2.setColor(Color.WHITE);
                g2.drawRect(x, y, barWidth, barHeight);

                int percentage = (int)((double)value / total * 100);
                g2.drawString(percentage + "%", x + 10, y - 5);
                g2.setFont(new Font("Arial", Font.PLAIN, 11));
                g2.drawString(cat, x - 5, height - padding + 15);
                g2.setFont(new Font("Arial", Font.BOLD, 14)); 
            }
            g2.setColor(Color.GRAY);
            g2.drawLine(padding, height - padding, width - padding, height - padding);
        }
    }

    class AdvancedGraphPanel extends JPanel implements MouseListener, MouseMotionListener {
        List<GraphNode> nodes = new ArrayList<>();
        javax.swing.Timer timer;
        GraphNode draggedNode = null;
        
        public AdvancedGraphPanel() {
            addMouseListener(this);
            addMouseMotionListener(this);
            timer = new javax.swing.Timer(16, e -> { updatePhysics(); repaint(); });
        }

        public void startSimulation() { if(!timer.isRunning()) timer.start(); }

        public void initMixedGraph(AnalyticsEngine analytics, Graph movieGraph) {
            nodes.clear();
            int centerX = getWidth() / 2; if(centerX == 0) centerX = 425;
            int centerY = getHeight() / 2; if(centerY == 0) centerY = 225;

            int totalInteractions = analytics.getTotalInteractions();
            String topCat = analytics.getTopCategory();
            Color centerColor = getCategoryColor(topCat);

            nodes.add(new GraphNode(centerX, centerY, "Interest Graph", true, null, centerColor));

            if (totalInteractions == 0) return; 

            int targetTotalNodes = 25; 
            Random rand = new Random();
            String[] categories = {"Hollywood", "Bollywood", "Korean", "Tollywood"};

            for (String cat : categories) {
                int count = analytics.userPreferences.getOrDefault(cat, 0);
                if (count == 0) continue;

                int numNodesForCat = (int) Math.round(((double)count / totalInteractions) * targetTotalNodes);
                if (numNodesForCat == 0 && count > 0) numNodesForCat = 1;

                List<Movie> catMovies = movieGraph.getMovies(cat);
                Collections.shuffle(catMovies);

                Color catColor = getCategoryColor(cat);

                for (int i = 0; i < Math.min(numNodesForCat, catMovies.size()); i++) {
                    Movie m = catMovies.get(i);
                    double angle = rand.nextDouble() * Math.PI * 2;
                    double dist = 100 + rand.nextInt(80); 
                    double nx = centerX + Math.cos(angle) * dist;
                    double ny = centerY + Math.sin(angle) * dist;
                    nodes.add(new GraphNode(nx, ny, m.title, false, m, catColor));
                }
            }
        }

        private void updatePhysics() {
            double repulsion = 4000; double springLength = 140; double springStrength = 0.05;
            if(nodes.isEmpty()) return;
            GraphNode centerNode = nodes.get(0);
            for (int i = 0; i < nodes.size(); i++) {
                GraphNode n1 = nodes.get(i);
                if (n1 == draggedNode) continue; 
                double fx = 0, fy = 0;
                for (int j = 0; j < nodes.size(); j++) {
                    if (i == j) continue;
                    GraphNode n2 = nodes.get(j);
                    double dx = n1.x - n2.x; double dy = n1.y - n2.y;
                    double distSq = dx*dx + dy*dy; if(distSq < 1) distSq = 1;
                    double force = repulsion / distSq;
                    double dist = Math.sqrt(distSq);
                    fx += (dx/dist) * force; fy += (dy/dist) * force;
                }
                if (!n1.isCategory) {
                    double dx = centerNode.x - n1.x; double dy = centerNode.y - n1.y;
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    double force = (dist - springLength) * springStrength;
                    fx += (dx/dist) * force; fy += (dy/dist) * force;
                } else {
                    double dx = (getWidth()/2.0) - n1.x; double dy = (getHeight()/2.0) - n1.y;
                    fx += dx * 0.05; fy += dy * 0.05;
                }
                n1.vx = (n1.vx + fx) * 0.90; n1.vy = (n1.vy + fy) * 0.90;
                n1.x += n1.vx; n1.y += n1.vy;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if(nodes.isEmpty()) return;
            g2.setStroke(new BasicStroke(2));
            g2.setColor(new Color(100, 100, 100));
            GraphNode center = nodes.get(0);
            for(int i=1; i<nodes.size(); i++) {
                GraphNode n = nodes.get(i);
                g2.drawLine((int)center.x, (int)center.y, (int)n.x, (int)n.y);
            }
            for(GraphNode n : nodes) {
                int size = n.isCategory ? 70 : 40; 
                int off = size / 2;
                
                g2.setColor(n.nodeColor);
                
                if(n.isCategory) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    g2.fillOval((int)n.x - off - 10, (int)n.y - off - 10, size + 20, size + 20);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
                
                g2.fillOval((int)n.x - off, (int)n.y - off, size, size);
                g2.setColor(Color.WHITE);
                g2.drawOval((int)n.x - off, (int)n.y - off, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                String label = n.label.length() > 10 ? n.label.substring(0,8)+".." : n.label;
                int strW = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, (int)n.x - strW/2, (int)n.y + size/2 + 15);
            }
            g2.setColor(Color.YELLOW);
            g2.drawString("Interactive Force Graph (Drag Nodes)", 20, 20);
        }

        @Override public void mousePressed(MouseEvent e) {
            for(GraphNode n : nodes) {
                double dx = e.getX() - n.x; double dy = e.getY() - n.y;
                if(Math.sqrt(dx*dx + dy*dy) < 30) { draggedNode = n; break; }
            }
        }
        @Override public void mouseDragged(MouseEvent e) {
            if(draggedNode != null) { draggedNode.x = e.getX(); draggedNode.y = e.getY(); draggedNode.vx = 0; draggedNode.vy = 0; }
        }
        @Override public void mouseReleased(MouseEvent e) { draggedNode = null; }
        @Override public void mouseClicked(MouseEvent e) {
            for(GraphNode n : nodes) {
                double dx = e.getX() - n.x; double dy = e.getY() - n.y;
                if(Math.sqrt(dx*dx + dy*dy) < 30) { if(!n.isCategory && n.movieData != null) playMovie(n.movieData); break; }
            }
        }
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mouseMoved(MouseEvent e) {}
    }

    private JPanel createSearchTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        JTextField box = new JTextField(20);
        JPanel top = new JPanel(); top.setBackground(BG);
        JLabel lbl = new JLabel("Search Movie: ");
        lbl.setForeground(Color.WHITE);
        top.add(lbl); top.add(box);
        JPanel res = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        res.setBackground(BG);
        box.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                res.removeAll();
                List<String> hits = trie.search(box.getText());
                for(String t : hits) {
                    Movie m = graph.findMovie(t);
                    if(m!=null) addCard(res, m, "ADD");
                }
                res.revalidate();
                res.repaint();
            }
        });
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(res), BorderLayout.CENTER);
        return p;
    }

    private JComponent createWatchTab() {
        watchlistPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        watchlistPanel.setBackground(BG);
        return new JScrollPane(watchlistPanel);
    }
    
    private void refreshWatchlist() {
        watchlistPanel.removeAll();
        for(Movie m : myWatchList) {
            addCard(watchlistPanel, m, "REMOVE");
        }
        watchlistPanel.revalidate();
        watchlistPanel.repaint();
        if(barChartPanel != null) barChartPanel.repaint();
    }

    private JComponent createHistoryTab() {
        historyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        historyPanel.setBackground(BG);
        return new JScrollPane(historyPanel);
    }

    private void refreshHistory() {
        historyPanel.removeAll();
        for(Movie m : myHistory) {
            addCard(historyPanel, m, "NONE"); 
        }
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private void addCard(JPanel parent, Movie m, String buttonType) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(140, 240));
        card.setBackground(ITEM_BG);
        card.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { playMovie(m); } });

        JLabel img = new JLabel("Loading...", SwingConstants.CENTER);
        img.setForeground(Color.GRAY);
        ImageLoader.load(m.imageUrl, m.title, img);

        JPanel info = new JPanel(new BorderLayout());
        info.setBackground(ITEM_BG);
        JLabel title = new JLabel("<html><center>"+m.title+"</center></html>", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.PLAIN, 11));
        info.add(title, BorderLayout.CENTER);

        if (buttonType.equals("ADD")) {
            JButton btn = new JButton("Add +");
            btn.setBackground(ACCENT);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("SansSerif", Font.BOLD, 10));
            btn.setFocusPainted(false);
            btn.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { e.consume(); } });
            btn.addActionListener(e -> {
                if(!myWatchList.contains(m)) {
                    myWatchList.add(m);
                    analytics.logAction(m.category); 
                    refreshWatchlist();
                    JOptionPane.showMessageDialog(frame, m.title + " added to Watchlist!");
                }
            });
            info.add(btn, BorderLayout.SOUTH);
        } 
        else if (buttonType.equals("REMOVE")) {
            JButton btn = new JButton("Remove");
            btn.setBackground(Color.DARK_GRAY);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("SansSerif", Font.BOLD, 10));
            btn.setFocusPainted(false);
            btn.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { e.consume(); } });
            btn.addActionListener(e -> {
                myWatchList.remove(m);
                refreshWatchlist(); 
            });
            info.add(btn, BorderLayout.SOUTH);
        }

        card.add(img, BorderLayout.CENTER);
        card.add(info, BorderLayout.SOUTH);
        parent.add(card);
    }

    private void loadData() {
        addM("Inception", "Hollywood", 8.8, "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_.jpg");
        addM("The Dark Knight", "Hollywood", 9.0, "https://m.media-amazon.com/images/M/MV5BMTMxNTMwODM0NF5BMl5BanBnXkFtZTcwODAyMTk2Mw@@._V1_.jpg");
        addM("Interstellar", "Hollywood", 8.6, "https://m.media-amazon.com/images/M/MV5BZjdkOTU3MDktN2IxOS00OGEyLWFmMjktY2FiMmZkNWIyODZiXkEyXkFqcGdeQXVyMTMxODk2OTU@._V1_.jpg");
        addM("Avengers Endgame", "Hollywood", 8.4, "https://m.media-amazon.com/images/M/MV5BMTc5MDE2ODcwNV5BMl5BanBnXkFtZTgwMzI2NzQ2NzM@._V1_.jpg");
        addM("Spider-Man NWH", "Hollywood", 8.2, "https://m.media-amazon.com/images/M/MV5BZWMyYzFjYTYtNTRjYi00OGExLWE2YzgtOGRmYjAxZTU3NzBiXkEyXkFqcGdeQXVyMzQ0MzA0NTM@._V1_.jpg");
        addM("Avatar", "Hollywood", 7.8, "https://m.media-amazon.com/images/M/MV5BZDA0OGQxNTItMDZkMC00N2UyLTg3MzMtYTJmNjg3Nzk5MzRiXkEyXkFqcGdeQXVyMjUzOTY1NTc@._V1_.jpg");
        addM("Titanic", "Hollywood", 7.9, "https://m.media-amazon.com/images/M/MV5BMDdmZGU3NDQtY2E5My00ZTliLWIzOTUtMTY4ZGI1YjdiNjk3XkEyXkFqcGdeQXVyNTA4NzY1MzY@._V1_.jpg");
        addM("Joker", "Hollywood", 8.1, "https://m.media-amazon.com/images/M/MV5BNGVjNWI4ZGUtNzE0MS00ZmJmLTljNzgtNDIyNjTkZmYyN2NmXkEyXkFqcGdeQXVyMDM2NDM2MQ@@._V1_.jpg");
        addM("Iron Man", "Hollywood", 7.9, "https://m.media-amazon.com/images/M/MV5BMTczNTI2ODUwOF5BMl5BanBnXkFtZTcwMTU0NTIzMw@@._V1_.jpg");
        addM("The Matrix", "Hollywood", 8.7, "https://m.media-amazon.com/images/M/MV5BNzQzOTk3OTAtNDQ0Zi00ZTVkLWI0MTEtMDllZjNkYzNjNTc4L2ltYWdlXkEyXkFqcGdeQXVyNjU0OTQ0OTY@._V1_.jpg");

        addM("3 Idiots", "Bollywood", 8.4, "https://m.media-amazon.com/images/M/MV5BNTkyOGVjMGEtNmQzZi00NzFlLTlhOWQtODYyMDc2ZGJmYzFhXkEyXkFqcGdeQXVyNjU0OTQ0OTY@._V1_.jpg");
        addM("Dangal", "Bollywood", 8.3, "https://m.media-amazon.com/images/M/MV5BMTQ4MzQzMzM2Nl5BMl5BanBnXkFtZTgwMTQ1NzU3MDI@._V1_.jpg");
        addM("Pathaan", "Bollywood", 7.0, "https://m.media-amazon.com/images/M/MV5BM2QzM2JiNTMtYjU4Ny00MDZkLTk3MmUtYTRjMzVkZGJlNmYyXkEyXkFqcGdeQXVyMTE0MzY0NjE1._V1_.jpg");
        addM("Jawan", "Bollywood", 7.2, "https://m.media-amazon.com/images/M/MV5BMmJlNTBmZDYtNzBjZi00MnlleLWJmMjktNWNiODAxNzdlOTYxXkEyXkFqcGdeQXVyMTE0MzY0NjE1._V1_.jpg");
        addM("PK", "Bollywood", 8.1, "https://m.media-amazon.com/images/M/MV5BMTYzOTE2NjkxN15BMl5BanBnXkFtZTgwMDgzMTg0MzE@._V1_.jpg");
        addM("Sholay", "Bollywood", 8.0, "https://m.media-amazon.com/images/M/MV5BNjFkMzA2MjItZTM4MC00MjM2LWI3YzYtMTAzZDI2NTA3ZTA5XkEyXkFqcGdeQXVyODE5NzE3OTE@._V1_.jpg"); 
        addM("Bajrangi Bhaijaan", "Bollywood", 8.0, "https://m.media-amazon.com/images/M/MV5BMTY0MzI3Mzg1MF5BMl5BanBnXkFtZTgwOTMzMDYwNTE@._V1_.jpg"); 
        addM("Lagaan", "Bollywood", 8.1, "https://m.media-amazon.com/images/M/MV5BNDYxNWUzZmYtOGQxMC00MTdkLTkxOTctYzkyOGIwNWQxZjhmXkEyXkFqcGdeQXVyNjU0OTQ0OTY@._V1_.jpg"); 
        addM("DDLJ", "Bollywood", 8.0, "https://m.media-amazon.com/images/M/MV5BMjY4ODkzNjE0OV5BMl5BanBnXkFtZTgwMTc0NTMwMDI@._V1_.jpg");
        addM("Drishyam", "Bollywood", 8.2, "https://m.media-amazon.com/images/M/MV5BYmJhZmJlYTItZmZlNy00MGY0LTg0ZGMtNWFkYWU5N2Y1YjdmXkEyXkFqcGdeQXVyMTEzNzg0Mjkx._V1_.jpg"); 

        addM("Parasite", "Korean", 8.5, "https://m.media-amazon.com/images/M/MV5BYWZjMjk3ZTItODQ2ZC00NTY5LWE0ZDYtZTI3MjcwN2Q5NTVkXkEyXkFqcGdeQXVyODk4OTc3MTY@._V1_.jpg");
        addM("Train to Busan", "Korean", 7.6, "https://m.media-amazon.com/images/M/MV5BMTkwOTQ4OTg0OV5BMl5BanBnXkFtZTgwMzQyOTM0OTE@._V1_.jpg");
        addM("Oldboy", "Korean", 8.4, "https://m.media-amazon.com/images/M/MV5BMTI3NTQyMzU5M15BMl5BanBnXkFtZTcwMTM2MjgyMQ@@._V1_.jpg"); 
        addM("Squid Game", "Korean", 8.0, "https://m.media-amazon.com/images/M/MV5BYWE3MDVkN2EtNjQ5MS00ZDQ4LTliNzYtMjc2YWMzODA1YmI1XkEyXkFqcGdeQXVyODk4OTc3MTY@._V1_.jpg"); 
        addM("Minari", "Korean", 7.5, "https://m.media-amazon.com/images/M/MV5BNjRkZjJiYWQtMTZmYS00ZmJjLWExMGMtYmNlYzhlNTQ5ZGPTXkEyXkFqcGdeQXVyODc0OTEyNDU@._V1_.jpg"); 
        addM("The Host", "Korean", 7.1, "https://m.media-amazon.com/images/M/MV5BMTkwOTQ4OTg0OV5BMl5BanBnXkFtZTgwMzQyOTM0OTE@._V1_.jpg"); 
        addM("Memories", "Korean", 8.1, "https://m.media-amazon.com/images/M/MV5BOGViNTQ4ODUtZGU2Yi00OTc3LTk0YzQtNWY2YWUyNmY0NTcwXkEyXkFqcGdeQXVyODc0OTEyNDU@._V1_.jpg"); 
        addM("Okja", "Korean", 7.3, "https://m.media-amazon.com/images/M/MV5BM2YxYmFjYWMtMzQ0My00MzQ2LTljYWMtM2E0Y2M2MTY2YWU4XkEyXkFqcGdeQXVyMTMxODk2OTU@._V1_.jpg"); 
        addM("Burning", "Korean", 7.5, "https://m.media-amazon.com/images/M/MV5BMTQ0ODQ3MzQ0Ml5BMl5BanBnXkFtZTgwOTIyODg2NTM@._V1_.jpg"); 
        addM("I Saw the Devil", "Korean", 7.8, "https://m.media-amazon.com/images/M/MV5BMjA1Nzc2ODk3NV5BMl5BanBnXkFtZTgwMjY3MjcwMTE@._V1_.jpg"); 

        addM("RRR", "Tollywood", 8.0, "https://m.media-amazon.com/images/M/MV5BODUwNDNjYzctODUxNy00ZTA2LWIyYTEtMDc5Y2E5ZjBmNTMzXkEyXkFqcGdeQXVyODE5NzE3OTE@._V1_.jpg");
        addM("Baahubali", "Tollywood", 8.0, "https://m.media-amazon.com/images/M/MV5BYWVlMjVhZWYtNWViNC00ODFkLTk1MmItYjU1MDY5ZDdhMTU3XkEyXkFqcGdeQXVyODIwMDI1NjM@._V1_.jpg");
        addM("Baahubali 2", "Tollywood", 8.2, "https://m.media-amazon.com/images/M/MV5BOGNlMmRkODYtMWRhZS00MzlpLWI2MGEtMzRiMWRlYjNmZGRmXkEyXkFqcGdeQXVyMTQxNzMzNDI@._V1_.jpg"); 
        addM("KGF 1", "Tollywood", 8.0, "https://m.media-amazon.com/images/M/MV5BZDNlNzBjMGUtYTA0Yy00OTI2LWJmZjMtODliYmUyYTI0OGFmXkEyXkFqcGdeQXVyODIwMDI1NjM@._V1_.jpg"); 
        addM("KGF 2", "Tollywood", 8.2, "https://m.media-amazon.com/images/M/MV5BMjA2M2Y4MzItZDBiMS00YTc5LThjMjQtMzA0NWM1ZjI1MWY5XkEyXkFqcGdeQXVyMTEzNzg0Mjkx._V1_.jpg"); 
        addM("Pushpa", "Tollywood", 7.6, "https://m.media-amazon.com/images/M/MV5BMmQ4YmM3NjgtNTExNC00ZTZhLWEwZTctYjdhOWI4ZWFlZjA2XkEyXkFqcGdeQXVyMTI1NDEyNTM5._V1_.jpg"); 
        addM("Eega", "Tollywood", 7.8, "https://m.media-amazon.com/images/M/MV5BOGE3ZWRkOTQtZDk1Mi00ZGU2LWEzNTItZTNkY2JmOTM4NjUyXkEyXkFqcGdeQXVyODIwMDI1NjM@._V1_.jpg"); 
        addM("Magadheera", "Tollywood", 7.7, "https://m.media-amazon.com/images/M/MV5BZmVjYWY1ZWMtZjA0MC00Yjg5LWIxN2ItOGU2MWMwY2E1MTc3XkEyXkFqcGdeQXVyNDY5MTUyNjU@._V1_.jpg"); 
        addM("Arjun Reddy", "Tollywood", 7.9, "https://m.media-amazon.com/images/M/MV5BZWYxYjI4OGQtNjI3YS00ZTk1LTk0YmQtNWRlNDIyMTE2ZjZlXkEyXkFqcGdeQXVyMTI1NDEyNTM5._V1_.jpg"); 
        addM("Vikram", "Tollywood", 8.3, "https://m.media-amazon.com/images/M/MV5BOTFkMTgxZmEtMWZkZS00NjAyLTg2ZjctYTE4M2U4YjY5NTM5XkEyXkFqcGdeQXVyMTI1NDEyNTM5._V1_.jpg"); 
    }

    private void addM(String t, String c, double r, String u) {
        Movie m = new Movie(t, c, r, u);
        trie.insert(t);
        graph.addMovie(m);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FilmForgeApp::new);
    }
}