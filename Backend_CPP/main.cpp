

#include <iostream>
#include <string>
#include <vector>
#include <thread> 
#include <chrono> 
#include <iomanip> 

// Importing Core Architecture Modules
#include "Movie.h"
#include "Graph.h"
#include "DataLoader.h"

using namespace std;

/**
 * [UTIL] Simulates Network Latency & Computational Overhead.
 * Used to mimic real-world server response times during data processing.
 * @param message: Log message to display during processing.
 */
void simulateProcessing(string message) {
    cout << message;
    for(int i = 0; i < 3; i++) {
        cout << ".";
        this_thread::sleep_for(chrono::milliseconds(200)); // Introducing synthetic delay
    }
    cout << " [OK]" << endl;
}

/**
 * [SERIALIZATION] JSON Response Builder.
 * Converts internal C++ Graph Nodes (Structs) into standard JSON format (RFC 8259).
 * This output is consumed by the Java Frontend via InputStream.
 */
void printJSONResponse(const vector<MovieNode>& movies) {
    cout << "\n[API] Serializing Data to JSON..." << endl;
    this_thread::sleep_for(chrono::milliseconds(500)); 

    // HTTP Header Simulation
    cout << "HTTP/1.1 200 OK" << endl;
    cout << "Content-Type: application/json; charset=utf-8" << endl;
    cout << "------------------------------------------------" << endl;
    
    // Begin JSON Array
    cout << "[" << endl;
    for (size_t i = 0; i < movies.size(); ++i) {
        const auto& m = movies[i];
        cout << "  {" << endl;
        cout << "    \"id\": \"" << m.id << "\"," << endl;
        cout << "    \"title\": \"" << m.title << "\"," << endl;
        cout << "    \"rating\": " << m.rating << "," << endl;
        cout << "    \"category\": \"" << m.category << "\"" << endl;
        
        // JSON Formatting: Append comma if not the last object
        if (i < movies.size() - 1) {
            cout << "  }," << endl;
        } else {
            cout << "  }" << endl;
        }
    }
    cout << "]" << endl; // End JSON Array
    cout << "------------------------------------------------" << endl;
}

/**
 * [MAIN] Application Entry Point.
 * Initializes the Graph Database and starts the Interactive CLI / API Listener.
 */
int main(int argc, char* argv[]) {
    // 1. [INIT] Graph Data Structure Initialization
    Graph movieGraph;
    cout << "\n==================================================" << endl;
    cout << "   FILMFORGE BACKEND SERVER | PORT: 8080 (VIRTUAL)   " << endl;
    cout << "   ENVIRONMENT: PRODUCTION | PID: " << _getpid() << endl;
    cout << "==================================================" << endl;

    // 2. [DAL] Data Access Layer - Hydrating Graph from File System
    simulateProcessing("[SYSTEM] Mounting Database (movies.txt)");
    DataLoader::loadMovies("movies.txt", movieGraph);

    // 3. [LISTENER] Request Dispatcher Loop
    // Listens for incoming commands (simulated via CLI input)
    int choice;
    while (true) {
        cout << "\nroot@server:~/api# "; // Simulating Linux Server Terminal
        
        // Fallback menu for manual debugging
        cout << "\n[1] GET /api/recommendations (Fetch Data)";
        cout << "\n[2] DEBUG: Inspect Graph Memory";
        cout << "\n[3] SHUTDOWN SERVER";
        cout << "\nSelect Action: ";
        cin >> choice;

        if (choice == 1) {
            cout << "\n[PARAM] Enter Category Filter (Hollywood, Bollywood, Korean, Tollywood): ";
            string cat;
            cin >> cat;

            // Log Incoming Request
            cout << "\n[NETWORK] Incoming Request: GET /api/recommend?category=" << cat << endl;
            simulateProcessing("[CORE] Traversing Adjacency List & Calculating Weights");
            
            // Fetch Recommendations via Hashing Algorithm
            vector<MovieNode> results = movieGraph.getRecommendations(cat);
            
            if (results.empty()) {
                // Return 404 JSON Response
                cout << "\n{ \"status\": 404, \"error\": \"Resource Not Found\" }" << endl;
            } else {
                // Serialize and Transmit Data
                printJSONResponse(results);
            }
        } 
        else if (choice == 2) {
            // Internal Memory Dump for Verification
            movieGraph.displayTopology();
        } 
        else {
            cout << "[SYSTEM] Terminating Process..." << endl;
            break;
        }
    }

    return 0;
}