#ifndef HISTORYDLL_H
#define HISTORYDLL_H

#include <iostream>
#include <fstream>
#include <vector>
#include "Structs.h"

using namespace std;

// DLL Node
struct HistoryNode {
    int movieId;
    HistoryNode *next;
    HistoryNode *prev;

    HistoryNode(int id) : movieId(id), next(nullptr), prev(nullptr) {}
};

class HistoryDLL {
    HistoryNode* head;
    HistoryNode* tail;

public:
    HistoryDLL() : head(nullptr), tail(nullptr) {}

    // Add to Front (Recently Watched Logic - O(1))
    void addRecent(int id) {
        HistoryNode* newNode = new HistoryNode(id);
        if (!head) {
            head = tail = newNode;
        } else {
            newNode->next = head;
            head->prev = newNode;
            head = newNode;
        }
    }

    // Load specific user history from file
    void loadUserHistory(string username) {
        ifstream file("history.txt");
        string u;
        int mid;
        
        // File mein purana data pehle hota hai, naya baad mein.
        // Hum simple append karte hain, lekin display karte waqt dhyan rakhenge.
        // Behtar hai ke hum file se read karke "addRecent" karein taake
        // jo aakhir mein read ho wo sabse upar (head) par aaye.
        
        while (file >> u >> mid) {
            if (u == username) {
                addRecent(mid); // Last entry in file becomes Head (Most Recent)
            }
        }
    }

    // Display History (Matches IDs with Main Database)
    void displayHistory(vector<Movie>& database) {
        HistoryNode* temp = head;
        while (temp) {
            // Find movie details from DB (Linear Search)
            for (const auto& m : database) {
                if (m.id == temp->movieId) {
                    cout << m.id << " " << m.title << " " << m.genre << " " << m.path << " " << m.views << endl;
                    break;
                }
            }
            temp = temp->next;
        }
    }

    // Destructor to clean memory
    ~HistoryDLL() {
        HistoryNode* curr = head;
        while (curr) {
            HistoryNode* next = curr->next;
            delete curr;
            curr = next;
        }
    }
};

#endif