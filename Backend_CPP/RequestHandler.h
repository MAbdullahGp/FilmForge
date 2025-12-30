#ifndef REQUESTHANDLER_H
#define REQUESTHANDLER_H
#include <string>
#include "Trie.h"
#include "Graph.h"
using namespace std;

class RequestHandler {
    Trie* trie;
    Graph* graph;

public:
    RequestHandler(Trie* t, Graph* g) : trie(t), graph(g) {}

    string handleRequest(string request) {
        if (request.find("SEARCH:") == 0) {
            string query = request.substr(7);
            vector<string> results = trie->autocomplete(query);
            
            string response = "";
            for (string s : results) response += s + ",";
            if (response == "") return "No match found";
            return response;
        } 
        else if (request.find("CAT:") == 0) {
            string category = request.substr(4);
            return graph->getRecommendations(category);
        }
        return "Invalid Command";
    }
};
#endif