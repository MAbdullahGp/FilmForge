#ifndef TRIE_H
#define TRIE_H
#include <vector>
#include "TrieNode.h"
#include "Utils.h"

class Trie {
    TrieNode* root;

    void searchPrefix(TrieNode* node, vector<string>& results) {
        if (node->isEndOfWord) {
            results.push_back(node->movieTitle);
        }
        for (auto pair : node->children) {
            searchPrefix(pair.second, results);
        }
    }

public:
    Trie() { root = new TrieNode(); }

    void insert(string title) {
        TrieNode* node = root;
        string lowerTitle = Utils::toLower(title);
        
        for (char ch : lowerTitle) {
            if (node->children.find(ch) == node->children.end()) {
                node->children[ch] = new TrieNode();
            }
            node = node->children[ch];
        }
        node->isEndOfWord = true;
        node->movieTitle = title;
    }

    vector<string> autocomplete(string prefix) {
        vector<string> results;
        TrieNode* node = root;
        string lowerPrefix = Utils::toLower(prefix);

        for (char ch : lowerPrefix) {
            if (node->children.find(ch) == node->children.end()) {
                return results; 
            }
            node = node->children[ch];
        }
        searchPrefix(node, results);
        return results;
    }
};
#endif