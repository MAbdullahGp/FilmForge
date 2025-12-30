#ifndef DATALOADER_H
#define DATALOADER_H

#include <fstream>
#include <sstream>
#include "Graph.h"

class DataLoader {
public:
    static void loadMovies(string filename, Graph& graph) {
        ifstream file(filename);
        if (!file.is_open()) {
            cout << "[Error] Database file '" << filename << "' not found!" << endl;
            return;
        }

        string line;
        int count = 0;
        while (getline(file, line)) {
            stringstream ss(line);
            string segment;
            vector<string> data;

            // Parsing logic: Split by '|'
            while (getline(ss, segment, '|')) {
                data.push_back(segment);
            }

            // Expected Format: ID|Title|Category|Rating|URL
            if (data.size() >= 5) {
                string title = data[1];
                string category = data[2];
                double rating = stod(data[3]);
                string url = data[4];

                graph.addEdge(category, title, rating, url);
                count++;
            }
        }
        file.close();
        cout << "[System] Data Parsing Complete. " << count << " nodes loaded into Graph Memory." << endl;
    }
};

#endif