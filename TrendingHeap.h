#ifndef TRENDINGHEAP_H
#define TRENDINGHEAP_H
#include "Structs.h"
#include <vector>
#include <algorithm>

class TrendingHeap {
    vector<Movie> heap;
public:
    void insert(Movie m) { heap.push_back(m); }
    void showTop() {
        // Simple sort for display (Simulating extraction from Max Heap)
        sort(heap.begin(), heap.end(), [](Movie a, Movie b){ return a.views > b.views; });
        for(int i=0; i<min((int)heap.size(), 10); i++) {
            cout << heap[i].id << " " << heap[i].title << " " << heap[i].genre << " " << heap[i].path << " " << heap[i].views << endl;
        }
    }
};
#endif