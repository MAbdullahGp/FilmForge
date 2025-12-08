#include <iostream>
#include <fstream>
#include <unordered_map>
#include <vector>
#include <queue>
#include <stack>
#include <algorithm>
#include <sstream>
#include <ctime>
using namespace std;

// ================ DATA STRUCTURES ================

// Hash Map for User Authentication
class UserAuth {
private:
    unordered_map<string, string> credentials; // username -> password
    
public:
    void loadUsers() {
        ifstream file("users.txt");
        string username, password;
        while(file >> username >> password) {
            credentials[username] = password;
        }
        file.close();
    }
    
    bool login(string username, string password) {
        if(credentials.find(username) != credentials.end()) {
            return credentials[username] == password;
        }
        return false;
    }
    
    bool registerUser(string username, string password) {
        if(credentials.find(username) != credentials.end()) {
            return false; // User already exists
        }
        credentials[username] = password;
        
        // Save to file
        ofstream file("users.txt", ios::app);
        file << username << " " << password << endl;
        file.close();
        return true;
    }
};

// Movie Structure
struct Movie {
    int id;
    string title;
    string genre;
    int year;
    float rating;
    int views;
    
    Movie(int i, string t, string g, int y, float r, int v) 
        : id(i), title(t), genre(g), year(y), rating(r), views(v) {}
};

// BST Node for Movie Catalog
struct BSTNode {
    Movie* movie;
    BSTNode* left;
    BSTNode* right;
    
    BSTNode(Movie* m) : movie(m), left(nullptr), right(nullptr) {}
};

// Binary Search Tree for Movie Catalog
class MovieCatalog {
private:
    BSTNode* root;
    
    BSTNode* insert(BSTNode* node, Movie* movie) {
        if(node == nullptr) return new BSTNode(movie);
        
        if(movie->id < node->movie->id)
            node->left = insert(node->left, movie);
        else
            node->right = insert(node->right, movie);
        
        return node;
    }
    
    BSTNode* search(BSTNode* node, int id) {
        if(node == nullptr || node->movie->id == id)
            return node;
        
        if(id < node->movie->id)
            return search(node->left, id);
        return search(node->right, id);
    }
    
    void inorder(BSTNode* node, vector<Movie*>& movies) {
        if(node == nullptr) return;
        inorder(node->left, movies);
        movies.push_back(node->movie);
        inorder(node->right, movies);
    }
    
public:
    MovieCatalog() : root(nullptr) {}
    
    void addMovie(Movie* movie) {
        root = insert(root, movie);
    }
    
    Movie* findMovie(int id) {
        BSTNode* result = search(root, id);
        return result ? result->movie : nullptr;
    }
    
    vector<Movie*> getAllMovies() {
        vector<Movie*> movies;
        inorder(root, movies);
        return movies;
    }
};

// Max Heap for Trending Movies
class TrendingMovies {
private:
    priority_queue<pair<int, Movie*>> heap; // views, movie
    
public:
    void addMovie(Movie* movie) {
        heap.push({movie->views, movie});
    }
    
    vector<Movie*> getTop(int n) {
        vector<Movie*> trending;
        priority_queue<pair<int, Movie*>> temp = heap;
        
        for(int i = 0; i < n && !temp.empty(); i++) {
            trending.push_back(temp.top().second);
            temp.pop();
        }
        return trending;
    }
};

// Trie for Search Autocomplete
struct TrieNode {
    unordered_map<char, TrieNode*> children;
    bool isEnd;
    string movieTitle;
    
    TrieNode() : isEnd(false) {}
};

class SearchTrie {
private:
    TrieNode* root;
    
    void collectWords(TrieNode* node, vector<string>& results) {
        if(node->isEnd) {
            results.push_back(node->movieTitle);
        }
        for(auto& pair : node->children) {
            collectWords(pair.second, results);
        }
    }
    
public:
    SearchTrie() { root = new TrieNode(); }
    
    void insert(string title) {
        TrieNode* curr = root;
        string lower = title;
        transform(lower.begin(), lower.end(), lower.begin(), ::tolower);
        
        for(char c : lower) {
            if(curr->children.find(c) == curr->children.end()) {
                curr->children[c] = new TrieNode();
            }
            curr = curr->children[c];
        }
        curr->isEnd = true;
        curr->movieTitle = title;
    }
    
    vector<string> autocomplete(string prefix) {
        vector<string> results;
        TrieNode* curr = root;
        string lower = prefix;
        transform(lower.begin(), lower.end(), lower.begin(), ::tolower);
        
        for(char c : lower) {
            if(curr->children.find(c) == curr->children.end())
                return results;
            curr = curr->children[c];
        }
        
        collectWords(curr, results);
        return results;
    }
};

// Linked List for Watch History
struct HistoryNode {
    int movieId;
    string timestamp;
    HistoryNode* next;
    HistoryNode* prev;
    
    HistoryNode(int id, string t) : movieId(id), timestamp(t), next(nullptr), prev(nullptr) {}
};

class WatchHistory {
private:
    unordered_map<string, HistoryNode*> userHistory; // username -> head
    
public:
    void addToHistory(string username, int movieId) {
        time_t now = time(0);
        string timestamp = ctime(&now);
        
        HistoryNode* newNode = new HistoryNode(movieId, timestamp);
        
        if(userHistory.find(username) == userHistory.end()) {
            userHistory[username] = newNode;
        } else {
            newNode->next = userHistory[username];
            userHistory[username]->prev = newNode;
            userHistory[username] = newNode;
        }
        
        // Save to file
        ofstream file("history_" + username + ".txt", ios::app);
        file << movieId << " " << timestamp;
        file.close();
    }
    
    vector<int> getHistory(string username) {
        vector<int> history;
        if(userHistory.find(username) == userHistory.end())
            return history;
        
        HistoryNode* curr = userHistory[username];
        while(curr != nullptr) {
            history.push_back(curr->movieId);
            curr = curr->next;
        }
        return history;
    }
};

// Graph for Recommendation System
class RecommendationGraph {
private:
    unordered_map<string, vector<string>> adjacencyList; // genre -> similar genres
    
public:
    void addEdge(string genre1, string genre2) {
        adjacencyList[genre1].push_back(genre2);
        adjacencyList[genre2].push_back(genre1);
    }
    
    vector<string> getRelatedGenres(string genre) {
        return adjacencyList[genre];
    }
    
    void buildGraph() {
        // Build relationships between genres
        addEdge("Action", "Thriller");
        addEdge("Action", "Adventure");
        addEdge("Comedy", "Romance");
        addEdge("Horror", "Thriller");
        addEdge("Sci-Fi", "Adventure");
        addEdge("Drama", "Romance");
    }
};

// Main System Class
class FilmForge {
public:
    UserAuth auth;
    MovieCatalog catalog;
    TrendingMovies trending;
    SearchTrie searchEngine;
    WatchHistory history;
    RecommendationGraph recGraph;
    vector<Movie*> allMovies;
    
    FilmForge() {
        auth.loadUsers();
        loadMovies();
        recGraph.buildGraph();
    }
    
    void loadMovies() {
        // Sample movies
        allMovies.push_back(new Movie(1, "Inception", "Sci-Fi", 2010, 8.8, 5000));
        allMovies.push_back(new Movie(2, "The Dark Knight", "Action", 2008, 9.0, 7000));
        allMovies.push_back(new Movie(3, "Interstellar", "Sci-Fi", 2014, 8.6, 4500));
        allMovies.push_back(new Movie(4, "The Matrix", "Action", 1999, 8.7, 6000));
        allMovies.push_back(new Movie(5, "Titanic", "Romance", 1997, 7.9, 8000));
        allMovies.push_back(new Movie(6, "The Shawshank Redemption", "Drama", 1994, 9.3, 9000));
        allMovies.push_back(new Movie(7, "Pulp Fiction", "Thriller", 1994, 8.9, 5500));
        allMovies.push_back(new Movie(8, "Forrest Gump", "Drama", 1994, 8.8, 7500));
        
        for(Movie* m : allMovies) {
            catalog.addMovie(m);
            trending.addMovie(m);
            searchEngine.insert(m->title);
        }
    }
    
    void processCommand(string command) {
        stringstream ss(command);
        string action;
        ss >> action;
        
        if(action == "LOGIN") {
            string username, password;
            ss >> username >> password;
            if(auth.login(username, password)) {
                cout << "SUCCESS:Login successful" << endl;
            } else {
                cout << "ERROR:Invalid credentials" << endl;
            }
        }
        else if(action == "REGISTER") {
            string username, password;
            ss >> username >> password;
            if(auth.registerUser(username, password)) {
                cout << "SUCCESS:Registration successful" << endl;
            } else {
                cout << "ERROR:Username already exists" << endl;
            }
        }
        else if(action == "SEARCH") {
            int id;
            ss >> id;
            Movie* m = catalog.findMovie(id);
            if(m) {
                cout << "MOVIE:" << m->id << "|" << m->title << "|" << m->genre 
                     << "|" << m->year << "|" << m->rating << "|" << m->views << endl;
            } else {
                cout << "ERROR:Movie not found" << endl;
            }
        }
        else if(action == "AUTOCOMPLETE") {
            string prefix;
            ss >> prefix;
            vector<string> results = searchEngine.autocomplete(prefix);
            cout << "SUGGESTIONS:";
            for(int i = 0; i < results.size(); i++) {
                cout << results[i];
                if(i < results.size() - 1) cout << "|";
            }
            cout << endl;
        }
        else if(action == "TRENDING") {
            int n;
            ss >> n;
            vector<Movie*> top = trending.getTop(n);
            cout << "TRENDING:";
            for(int i = 0; i < top.size(); i++) {
                cout << top[i]->id << "," << top[i]->title << "," << top[i]->views;
                if(i < top.size() - 1) cout << "|";
            }
            cout << endl;
        }
        else if(action == "ALLMOIVES") {
            vector<Movie*> movies = catalog.getAllMovies();
            cout << "ALLMOVIES:";
            for(int i = 0; i < movies.size(); i++) {
                cout << movies[i]->id << "," << movies[i]->title << "," 
                     << movies[i]->genre << "," << movies[i]->year << "," 
                     << movies[i]->rating;
                if(i < movies.size() - 1) cout << "|";
            }
            cout << endl;
        }
        else if(action == "ADDHISTORY") {
            string username;
            int movieId;
            ss >> username >> movieId;
            history.addToHistory(username, movieId);
            cout << "SUCCESS:Added to history" << endl;
        }
        else if(action == "GETHISTORY") {
            string username;
            ss >> username;
            vector<int> hist = history.getHistory(username);
            cout << "HISTORY:";
            for(int i = 0; i < hist.size(); i++) {
                cout << hist[i];
                if(i < hist.size() - 1) cout << "|";
            }
            cout << endl;
        }
        else if(action == "RECOMMEND") {
            string genre;
            ss >> genre;
            vector<string> related = recGraph.getRelatedGenres(genre);
            cout << "RELATED:";
            for(int i = 0; i < related.size(); i++) {
                cout << related[i];
                if(i < related.size() - 1) cout << "|";
            }
            cout << endl;
        }
        else {
            cout << "ERROR:Unknown command" << endl;
        }
    }
};

int main() {
    FilmForge system;
    string command;
    
    while(getline(cin, command)) {
        if(command == "EXIT") break;
        system.processCommand(command);
    }
    
    return 0;
}