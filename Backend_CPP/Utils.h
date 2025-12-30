#ifndef UTILS_H
#define UTILS_H
#include <string>
#include <algorithm>
#include <cctype>
using namespace std;

class Utils {
public:
    static string toLower(string s) {
        transform(s.begin(), s.end(), s.begin(), ::tolower);
        return s;
    }
};
#endif             