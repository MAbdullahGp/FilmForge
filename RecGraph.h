#ifndef RECGRAPH_H
#define RECGRAPH_H
#include "Structs.h"
#include <map>
#include <fstream>

class RecGraph {
public:
    void recommend(string username, vector<Movie>& allMovies) {
        ifstream hFile("history.txt");
        string u; int mid;
        map<string, int> genreWeights;
        vector<int> watched;

        while(hFile >> u >> mid) {
            if(u == username) {
                watched.push_back(mid);
                for(auto& m : allMovies) if(m.id == mid) { genreWeights[m.genre]++; break; }
            }
        }
        string topG = ""; int maxW = -1;
        for(auto const& [g, w] : genreWeights) if(w > maxW) { maxW = w; topG = g; }

        cout << "TOP_GENRE:" << topG << endl;
        for(auto& m : allMovies) {
            bool seen = false;
            for(int id : watched) if(id == m.id) seen = true;
            if(!seen && m.genre == topG) cout << m.id << " " << m.title << " " << m.genre << " " << m.path << " " << m.views << endl;
        }
    }
};
#endif