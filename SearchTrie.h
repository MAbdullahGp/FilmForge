#ifndef SEARCHTRIE_H
#define SEARCHTRIE_H

#include "Structs.h"
#include <unordered_map>
#include <vector>
#include <string>
#include <algorithm> // For transform (lowercase)

using namespace std;

// Trie Node Structure
struct TrieNode {
    unordered_map<char, TrieNode*> children;
    bool isEndOfWord;
    vector<int> movieIds; // Stores IDs of movies that match this word

    TrieNode() : isEndOfWord(false) {}
};

class SearchTrie {
    TrieNode* root;

    // Helper: Recursively find all movie IDs under a specific node
    // (Used for Autocomplete: "Ave" -> "Avengers", "Avatar")
    void collectAll(TrieNode* node, vector<int>& results) {
        if (!node) return;
        
        if (node->isEndOfWord) {
            for (int id : node->movieIds) {
                results.push_back(id);
            }
        }
        
        for (auto& pair : node->children) {
            collectAll(pair.second, results);
        }
    }

public:
    SearchTrie() {
        root = new TrieNode();
    }

    // Insert a movie title into the Trie
    void insert(string title, int id) {
        TrieNode* curr = root;
        
        // Convert to lowercase for case-insensitive search
        string temp = title;
        transform(temp.begin(), temp.end(), temp.begin(), ::tolower);
        
        for (char c : temp) {
            if (curr->children.find(c) == curr->children.end()) {
                curr->children[c] = new TrieNode();
            }
            curr = curr->children[c];
        }
        curr->isEndOfWord = true;
        curr->movieIds.push_back(id);
    }

    // Search for any title starting with 'prefix'
    vector<int> searchPrefix(string prefix) {
        TrieNode* curr = root;
        
        // Convert input to lowercase
        transform(prefix.begin(), prefix.end(), prefix.begin(), ::tolower);

        for (char c : prefix) {
            if (curr->children.find(c) == curr->children.end()) {
                return {}; // No match found
            }
            curr = curr->children[c];
        }
        
        // We found the prefix node, now collect all movies below this point
        vector<int> results;
        collectAll(curr, results);
        return results;
    }
};

#endif