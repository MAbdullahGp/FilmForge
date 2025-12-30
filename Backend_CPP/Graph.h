#ifndef GRAPH_H
#define GRAPH_H
#include <map>
#include <list>
#include <string>
#include "Movie.h"
using namespace std;

class Graph {
    map<string, list<Movie>> adjList;

public:
    void addMovie(Movie m) {
        adjList[m.category].push_back(m);
    }

    string getRecommendations(string category) {
        string result = "";
        if (adjList.find(category) != adjList.end()) {
            for (auto const& m : adjList[category]) {
                // Format: Title|Rating|URL ; Title|Rating|URL
                result += m.title + "|" + to_string(m.rating) + "|" + m.url + ";";
            }
        } else {
            result = "None";
        }
        return result;
    }
};
#endif