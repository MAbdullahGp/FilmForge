#ifndef SEARCHENGINE_H
#define SEARCHENGINE_H

#include <iostream>
#include <vector>
#include <string>
#include <unordered_map>
#include <algorithm>
#include <sstream>
#include "Structs.h" // <-- Yahan se Movie struct aayega (No Redefinition)

using namespace std;

class SearchEngine {
private:
    // Hashing: Keyword -> List of Movies
    unordered_map<string, vector<Movie>> searchIndex;

    string toLower(string s) {
        transform(s.begin(), s.end(), s.begin(), ::tolower);
        return s;
    }

public:
    void buildIndex(const vector<Movie>& allMovies) {
        searchIndex.clear();
        for (const auto& movie : allMovies) {
            string title = toLower(movie.title);
            string genre = toLower(movie.genre);
            
            // Index by full title
            searchIndex[title].push_back(movie);

            // Index by individual words in title
            stringstream ss(title);
            string word;
            while (ss >> word) {
                searchIndex[word].push_back(movie);
            }
            
            // Index by genre
            searchIndex[genre].push_back(movie);
        }
    }

    vector<Movie> search(string query) {
        query = toLower(query);
        vector<Movie> results;

        if (searchIndex.find(query) != searchIndex.end()) {
            results = searchIndex[query];
        } else {
            // Partial match fallback
            for (auto const& [key, val] : searchIndex) {
                if (key.find(query) != string::npos) {
                    results.insert(results.end(), val.begin(), val.end());
                }
            }
        }
        
        // Remove duplicates based on ID
        sort(results.begin(), results.end(), [](const Movie& a, const Movie& b) {
            return a.id < b.id;
        });
        auto last = unique(results.begin(), results.end(), [](const Movie& a, const Movie& b) {
            return a.id == b.id;
        });
        results.erase(last, results.end());

        return results;
    }
};

#endif