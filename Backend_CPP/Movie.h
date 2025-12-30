#ifndef GRAPH_H
#define GRAPH_H

#include <unordered_map>
#include <vector>
#include <algorithm>
#include "Movie.h"

using namespace std;

class Graph {
private:
    // HASH MAP: Category -> List of Movies
    unordered_map<string, vector<MovieNode>> adjList;

public:
    // Add Node to Graph
    void addEdge(string category, string title, double rating, string url) {
        MovieNode newNode(title, category, rating, url);
        adjList[category].push_back(newNode);
    }

    // Recommendation Logic (Sorting + Traversal)
    vector<MovieNode> getRecommendations(string category) {
        cout << "[Backend Logic] Hashing Key: '" << category << "' -> Accessing Memory Bucket..." << endl;
        
        if (adjList.find(category) == adjList.end()) {
            return {}; // Return empty if category not found
        }

        vector<MovieNode> movies = adjList[category];

        // Algorithm: Sort by Rating (High to Low)
        sort(movies.begin(), movies.end(), [](const MovieNode& a, const MovieNode& b) {
            return a.rating > b.rating; 
        });

        return movies;
    }

    // For Viva Demo: Show Internal Structure
    void displayTopology() {
        cout << "\n===== INTERNAL GRAPH STRUCTURE (ADJACENCY LIST) =====" << endl;
        for (auto const& [cat, movies] : adjList) {
            cout << "[ HEAD NODE: " << cat << " ] connects to -> " << movies.size() << " Neighbors" << endl;
            for (const auto& m : movies) {
                cout << "    |__ (Edge Weight: " << m.rating << ") --> [MovieID: " << m.id << " | " << m.title << "]" << endl;
            }
            cout << "-----------------------------------------------------" << endl;
        }
    }
};

#endif