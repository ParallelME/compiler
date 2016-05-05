#ifndef UTIL_STRING_HPP
#define UTIL_STRING_HPP

#include <algorithm>
#include <cctype>
#include <functional>
#include <locale>
#include <string>

/*
 * All functions on this file alter the given string and return it for
 * convenience.
 */

/// trim from start
inline std::string &ltrim(std::string &s) {
        s.erase(s.begin(), std::find_if(s.begin(), s.end(),
                    std::not1(std::ptr_fun<int, int>(std::isspace))));
        return s;
}

/// trim from end
inline std::string &rtrim(std::string &s) {
        s.erase(std::find_if(s.rbegin(), s.rend(),
                    std::not1(std::ptr_fun<int, int>(std::isspace))).base(), s.end());
        return s;
}

/// trim from both ends
inline std::string &trim(std::string &s) {
        return ltrim(rtrim(s));
}

/// Remove '\n' and '\r' from strings.
inline std::string &removeNewlines(std::string &s) {
    s.erase(std::remove(s.begin(), s.end(), '\n'), s.end());
    s.erase(std::remove(s.begin(), s.end(), '\r'), s.end());
    return s;
}

#endif // !UTIL_STRING_HPP
