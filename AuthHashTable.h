#ifndef AUTH_H
#define AUTH_H
#include <fstream>
#include "Structs.h"

struct UserNode {
    string username;
    string password;
    UserNode* next;
    UserNode(string u, string p) : username(u), password(p), next(nullptr) {}
};

class AuthHashTable {
    static const int TABLE_SIZE = 100;
    UserNode* table[TABLE_SIZE];

    int hashFunction(string key) {
        int sum = 0;
        for (char c : key) sum += c;
        return sum % TABLE_SIZE;
    }

public:
    AuthHashTable() {
        for (int i = 0; i < TABLE_SIZE; i++) table[i] = nullptr;
        loadUsers();
    }

    void loadUsers() {
        ifstream file("users.txt");
        string u, p;
        while (file >> u >> p) {
            addUserToTable(u, p);
        }
    }

    void addUserToTable(string u, string p) {
        int idx = hashFunction(u);
        UserNode* newNode = new UserNode(u, p);
        newNode->next = table[idx];
        table[idx] = newNode;
    }

    bool login(string u, string p) {
        int idx = hashFunction(u);
        UserNode* curr = table[idx];
        while (curr) {
            if (curr->username == u && curr->password == p) return true;
            curr = curr->next;
        }
        return false;
    }

    void registerUser(string u, string p) {
        if (login(u, p)) return; // Already exists
        
        // Add to Memory (Hash Table)
        addUserToTable(u, p);
        
        // Add to File
        ofstream file("users.txt", ios::app);
        file << u << " " << p << endl;
    }
};
#endif