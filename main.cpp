#include <iostream>
#include <vector>
#include <fstream>
#include <sstream>

// --- ALL DSA HEADERS ---
#include "Structs.h"       // Movie struct
#include "AuthHashTable.h" // Auth
#include "Sorting.h"       // Sort
#include "MovieAVL.h"      // Tree
#include "TrendingHeap.h"  // Heap
#include "RecGraph.h"      // Graph
#include "SearchTrie.h"    // Trie
#include "HistoryDLL.h"    // Linked List
#include "SearchEngine.h"  // <-- Hashing Search Wrapper

using namespace std;

vector<Movie> database;

// Load Data Function
void loadDB() {
    ifstream file("movies.txt");
    if (!file.is_open()) {
        cerr << "Error: Could not open movies.txt" << endl;
        return;
    }

    string line;
    while (getline(file, line)) {
        if(line.empty()) continue;
        stringstream ss(line);
        Movie m;

        // Assumes file format: ID Title Genre Path Views
        // Example in file: 101 The_Dark_Knight Action videos/tdk.mp4 5000
        ss >> m.id >> m.title >> m.genre >> m.path >> m.views;
        
        // FIX: Convert underscores in title back to spaces for better searching
        // So "The_Dark_Knight" becomes "The Dark Knight"
        for(char &c : m.title) {
            if(c == '_') c = ' ';
        }
        
        database.push_back(m);
    }
    file.close();
}


int main(int argc, char* argv[]) {
    // 1. Load Data
    loadDB();
    
    // 2. Initialize Structures
    AuthHashTable auth;
    MovieAVL tree;
    TrendingHeap heap;
    RecGraph graph;
    HistoryDLL history;
    SearchTrie trieSearch; 
    SearchEngine hashSearch; // Your Hashing Class

    // 3. Populate Data into Structures
    for(auto& m : database) {
        tree.add(m);
        heap.insert(m);
        trieSearch.insert(m.title, m.id); 
    }
    
    // 4. Build Fast Hash Index (CRITICAL STEP)
    hashSearch.buildIndex(database);

    // 5. Command Handling
    if (argc < 2) return 0;
    string cmd = argv[1];

    if (cmd == "login") {
        if (argc >= 4) {
            if (auth.login(argv[2], argv[3])) cout << "SUCCESS" << endl;
            else cout << "FAIL" << endl;
        }
    }
    else if (cmd == "register") {
        if (argc >= 4) {
            auth.registerUser(argv[2], argv[3]);
            cout << "SUCCESS" << endl;
        }
    }
    else if (cmd == "load_all") {
        tree.display(); // AVL traversal (Ensure this prints ID Title Genre Path Views)
    }
    else if (cmd == "trending") {
        heap.showTop(); // Max Heap
    }
    else if (cmd == "sort_views") {
        Sorter::mergeSort(database, 0, database.size()-1);
        for(auto& m : database) 
            cout << m.id << " " << m.title << " " << m.genre << " " << m.path << " " << m.views << endl;
    }
    else if (cmd == "sort_name") {
        Sorter::quickSort(database, 0, database.size()-1);
        for(auto& m : database) 
            cout << m.id << " " << m.title << " " << m.genre << " " << m.path << " " << m.views << endl;
    }
    else if (cmd == "recommend") {
        if(argc >= 3) graph.recommend(argv[2], database);
    }
    else if (cmd == "search") {
        if(argc >= 3) {
            // FIX: Combine all remaining args to form the full Movie Name
            // e.g., "The" "Dark" "Knight" -> "The Dark Knight"
            string query = "";
            for (int i = 2; i < argc; ++i) {
                query += argv[i];
                if (i < argc - 1) query += " "; // Add space between words
            }
            
            // HASHING SEARCH (O(1))
            vector<Movie> results = hashSearch.search(query);
            
            if (results.empty()) {
                // Optional: Fallback to Trie logic here if needed
                cout << "FAIL" << endl;
            } else {
                for(auto& m : results) {
                    // EXACT FORMAT REQUIRED BY JAVA PARSE FUNCTION
                    cout << m.id << " " << m.title << " " << m.genre << " " << m.path << " " << m.views << endl;
                }
            }
        }
    }
    else if (cmd == "history") {
        if (argc >= 3) {
            history.loadUserHistory(argv[2]);
            history.displayHistory(database);
        }
    }

    return 0;
}