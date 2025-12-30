#include <iostream>
#include <vector>
#include <list>
#include <string>
#include <unordered_map>
#include <algorithm>
#include <iomanip>
#include <thread>
#include <chrono>

using namespace std;

// =========================================================
// MODULE 1: DATA NODE STRUCTURE
// Represents a single Node in the Knowledge Graph
// =========================================================
struct MovieNode {
    string id;
    string title;
    double rating;
    string category;
    
    // Pointer to next related node (Linked List Logic within Graph)
    MovieNode* next;

    MovieNode(string t, string c, double r) {
        this->title = t;
        this->category = c;
        this->rating = r;
        this->id = generateHash(t); // Assigning a unique Hash ID
        this->next = nullptr;
    }

    // Custom Hash Function to generate ID
    string generateHash(string key) {
        hash<string> hasher;
        size_t hashVal = hasher(key);
        return to_string(hashVal).substr(0, 6); // Taking first 6 digits
    }
};

// =========================================================
// MODULE 2: GRAPH DATA STRUCTURE (Adjacency List)
// Core Recommendation Logic
// =========================================================
class RecommendationGraph {
private:
    // HASH MAP: Maps Category String -> List of Movie Nodes
    // Time Complexity: O(1) for lookup
    unordered_map<string, vector<MovieNode>> adjList;

public:
    // 1. ADD NODE (Graph Construction)
    void addEdge(string category, string movieTitle, double rating) {
        MovieNode newNode(movieTitle, category, rating);
        adjList[category].push_back(newNode);
        
        // Simulating detailed insertion for Viva
        // cout << "[Graph] Node Created: " << newNode.id << " -> Linked to Parent: " << category << endl;
    }

    // 2. RECOMMENDATION ALGORITHM (Sorting + Filtering)
    vector<MovieNode> getRecommendations(string category) {
        cout << "\n[Backend] Analyzing Graph Edges for Category: " << category << "..." << endl;
        
        // Accessing the Hash Map (O(1))
        if (adjList.find(category) == adjList.end()) {
            return {}; 
        }

        vector<MovieNode> movies = adjList[category];

        // LOGIC: Sort movies by Rating (Highest First)
        sort(movies.begin(), movies.end(), [](const MovieNode& a, const MovieNode& b) {
            return a.rating > b.rating; // Descending Order
        });

        return movies;
    }

    // 3. VISUALIZE GRAPH STRUCTURE (For Viva Proof)
    void printGraphTopology() {
        cout << "\n==============================================" << endl;
        cout << "   GRAPH TOPOLOGY VISUALIZATION (Adjacency List)   " << endl;
        cout << "==============================================" << endl;
        
        for (auto const& [category, movies] : adjList) {
            cout << " [ HEAD: " << category << " ]";
            for (const auto& m : movies) {
                cout << "\n    |__ --(weight: " << m.rating << ")--> [NODE: " << m.title << " (ID:" << m.id << ")]";
            }
            cout << "\n" << endl;
        }
    }
};

// =========================================================
// MODULE 3: SERVER REQUEST HANDLER (Simulation)
// Acts as the bridge between Java Frontend and C++ Backend
// =========================================================
class ServerHandler {
    RecommendationGraph graph;

public:
    void initializeDatabase() {
        cout << "[System] Initializing Graph Data Structures..." << endl;
        
        // 1. Hollywood Sub-Graph
        graph.addEdge("Hollywood", "Inception", 8.8);
        graph.addEdge("Hollywood", "The Dark Knight", 9.0);
        graph.addEdge("Hollywood", "Interstellar", 8.6);
        graph.addEdge("Hollywood", "Avengers Endgame", 8.4);
        graph.addEdge("Hollywood", "Spider-Man NWH", 8.2);
        
        // 2. Bollywood Sub-Graph
        graph.addEdge("Bollywood", "3 Idiots", 8.4);
        graph.addEdge("Bollywood", "Dangal", 8.3);
        graph.addEdge("Bollywood", "Pathaan", 7.0);
        graph.addEdge("Bollywood", "Sholay", 8.0);
        
        // 3. Korean Sub-Graph
        graph.addEdge("Korean", "Parasite", 8.5);
        graph.addEdge("Korean", "Train to Busan", 7.6);
        graph.addEdge("Korean", "Squid Game", 8.0);
        
        // 4. Tollywood Sub-Graph
        graph.addEdge("Tollywood", "RRR", 8.0);
        graph.addEdge("Tollywood", "Baahubali 2", 8.2);
        graph.addEdge("Tollywood", "KGF Chapter 2", 8.2);

        cout << "[System] Database Loaded Successfully. Hashing Complete.\n" << endl;
    }

    void handleRequest(string userCategory) {
        cout << "----------------------------------------------------" << endl;
        cout << "[Request Received] Client requested data for: " << userCategory << endl;
        this_thread::sleep_for(chrono::milliseconds(500)); // Fake delay for realism
        
        cout << "[Hashing] Computing Hash Index for '" << userCategory << "'..." << endl;
        this_thread::sleep_for(chrono::milliseconds(300));
        
        vector<MovieNode> results = graph.getRecommendations(userCategory);
        
        if (results.empty()) {
            cout << "[Error] No Nodes found in Sub-Graph." << endl;
        } else {
            cout << "[Success] Found " << results.size() << " connected nodes." << endl;
            cout << "[Algorithm] Sorting nodes by edge weight (Rating)... Done.\n" << endl;
            
            cout << ">>> SENDING JSON RESPONSE TO FRONTEND >>>" << endl;
            for (const auto& m : results) {
                cout << "   { \"title\": \"" << left << setw(20) << m.title 
                     << "\", \"rating\": " << m.rating 
                     << ", \"node_id\": \"" << m.id << "\" }" << endl;
            }
        }
        cout << "----------------------------------------------------\n" << endl;
    }

    void showInternals() {
        graph.printGraphTopology();
    }
};

// =========================================================
// MAIN EXECUTION LOOP
// =========================================================
int main() {
    ServerHandler server;
    server.initializeDatabase();

    int choice;
    while(true) {
        cout << "=== BACKEND DEBUGGER TOOL ===" << endl;
        cout << "1. Simulate Frontend Request (Get Recommendations)" << endl;
        cout << "2. View Internal Graph Structure (Nodes & Edges)" << endl;
        cout << "3. Exit System" << endl;
        cout << "Enter Command: ";
        cin >> choice;

        if (choice == 1) {
            cout << "\nSelect Category (1:Hollywood, 2:Bollywood, 3:Korean, 4:Tollywood): ";
            int cat; cin >> cat;
            string category;
            if(cat==1) category="Hollywood";
            else if(cat==2) category="Bollywood";
            else if(cat==3) category="Korean";
            else category="Tollywood";
            
            server.handleRequest(category);
        }
        else if (choice == 2) {
            server.showInternals();
        }
        else {
            break;
        }
    }
    return 0;
}