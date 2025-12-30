#ifndef TRIENODE_H
#define TRIENODE_H
#include <unordered_map>
#include <string>
using namespace std;

class TrieNode {
public:
    unordered_map<char, TrieNode*> children;
    bool isEndOfWord;
    string movieTitle;

    TrieNode() { isEndOfWord = false; }
};
#endif