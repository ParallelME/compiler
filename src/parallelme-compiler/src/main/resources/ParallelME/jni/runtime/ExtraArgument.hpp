#ifndef RUNTIME_EXTRAARGUMENT_H
#define RUNTIME_EXTRAARGUMENT_H

union Primitive{
    char c;
    int i;
    unsigned char uc;
    float f;
    short s;
};

//I need this 'class' modifier because enums doesnt have scope.
//So I was getting conflicts with AllocationType enum.
enum class ArgType {
    CHAR, INT, UCHAR, FLOAT, SHORT
};

class ExtraArgument{
public:
    Primitive value;
    ArgType argType;
};

#endif //RUNTIME_EXTRAARGUMENT_H