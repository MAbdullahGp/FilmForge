#ifndef MOVIEAVL_H
#define MOVIEAVL_H
#include "Structs.h"
#include <algorithm>

struct AVLNode {
    Movie data;
    AVLNode *left, *right;
    int height;
    AVLNode(Movie m) : data(m), left(nullptr), right(nullptr), height(1) {}
};

class MovieAVL {
    AVLNode* root = nullptr;
    int height(AVLNode* N) { return (N == nullptr) ? 0 : N->height; }
    int getBalance(AVLNode* N) { return (N == nullptr) ? 0 : height(N->left) - height(N->right); }

    AVLNode* rightRotate(AVLNode* y) {
        AVLNode* x = y->left; AVLNode* T2 = x->right;
        x->right = y; y->left = T2;
        y->height = max(height(y->left), height(y->right)) + 1;
        x->height = max(height(x->left), height(x->right)) + 1;
        return x;
    }
    AVLNode* leftRotate(AVLNode* x) {
        AVLNode* y = x->right; AVLNode* T2 = y->left;
        y->left = x; x->right = T2;
        x->height = max(height(x->left), height(x->right)) + 1;
        y->height = max(height(y->left), height(y->right)) + 1;
        return y;
    }
    AVLNode* insert(AVLNode* node, Movie key) {
        if (!node) return new AVLNode(key);
        if (key.id < node->data.id) node->left = insert(node->left, key);
        else if (key.id > node->data.id) node->right = insert(node->right, key);
        else return node;
        node->height = 1 + max(height(node->left), height(node->right));
        int bal = getBalance(node);
        if (bal > 1 && key.id < node->left->data.id) return rightRotate(node);
        if (bal < -1 && key.id > node->right->data.id) return leftRotate(node);
        if (bal > 1 && key.id > node->left->data.id) { node->left = leftRotate(node->left); return rightRotate(node); }
        if (bal < -1 && key.id < node->right->data.id) { node->right = rightRotate(node->right); return leftRotate(node); }
        return node;
    }
    void inOrder(AVLNode* root) {
        if(root) { inOrder(root->left); cout << root->data.id << " " << root->data.title << " " << root->data.genre << " " << root->data.path << " " << root->data.views << endl; inOrder(root->right); }
    }
public:
    void add(Movie m) { root = insert(root, m); }
    void display() { inOrder(root); }
};
#endif
//void inOrder(AVLNode* root) {
   //     if(root) { inOrder(root->left); cout << root->data.id << " " << root->data.title << " " << root->data.genre << " " << root->data.path << " " << root->data.views << endl; inOrder(root->right); }
    //}
//public:
    //void add(Movie m) { root = insert(root, m); }
    //givoid display() { inOrder(root); }