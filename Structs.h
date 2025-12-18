#ifndef STRUCTS_H
#define STRUCTS_H
#include <string>
#include <iostream>
#include <vector>
using namespace std;

struct Movie {
    int id;
    string title;
    string genre;
    string path;
    int views;

    Movie() {}
    Movie(int i, string t, string g, string p, int v) 
        : id(i), title(t), genre(g), path(p), views(v) {}
};
#endif