#ifndef SORTING_H
#define SORTING_H
#include "Structs.h"
#include <vector>

class Sorter {
    // Merge Sort Helper (Views - Descending)
    static void merge(vector<Movie>& arr, int l, int m, int r) {
        int n1 = m - l + 1, n2 = r - m;
        vector<Movie> L(n1), R(n2);
        for(int i=0; i<n1; i++) L[i] = arr[l+i];
        for(int j=0; j<n2; j++) R[j] = arr[m+1+j];
        int i=0, j=0, k=l;
        while(i<n1 && j<n2) {
            if(L[i].views >= R[j].views) arr[k++] = L[i++]; 
            else arr[k++] = R[j++];
        }
        while(i<n1) arr[k++] = L[i++];
        while(j<n2) arr[k++] = R[j++];
    }

    // Quick Sort Helper (Title - Ascending)
    static int partition(vector<Movie>& arr, int low, int high) {
        string pivot = arr[high].title;
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            if (arr[j].title < pivot) {
                i++; swap(arr[i], arr[j]);
            }
        }
        swap(arr[i + 1], arr[high]);
        return (i + 1);
    }

public:
    static void mergeSort(vector<Movie>& arr, int l, int r) {
        if (l >= r) return;
        int m = l + (r - l) / 2;
        mergeSort(arr, l, m);
        mergeSort(arr, m + 1, r);
        merge(arr, l, m, r);
    }

    static void quickSort(vector<Movie>& arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }
};
#endif