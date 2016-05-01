//
// Created by pedro on 4/7/16.
//

#ifndef RUNTIME_ALLOCATIONTYPE_H
#define RUNTIME_ALLOCATIONTYPE_H

class AllocationType {
    int index;
public:
    static constexpr int length() {return 9;}
    AllocationType() : index(0) {}
    constexpr explicit AllocationType(int index) : index(index) {}
    constexpr operator int() const { return index; }

    int size() const;

};

//These values are the size in bytes of these types in Java
//See: https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html
static constexpr AllocationType ALLOCATION_TYPE_BOOLEAN(0); //I probably can't create a bit so I'm giving a byte;
static constexpr AllocationType ALLOCATION_TYPE_BYTE(1);
static constexpr AllocationType ALLOCATION_TYPE_CHAR(2); //Java char have 2 bytes
static constexpr AllocationType ALLOCATION_TYPE_SHORT(3);
static constexpr AllocationType ALLOCATION_TYPE_INT(4);
static constexpr AllocationType ALLOCATION_TYPE_LONG(5);
static constexpr AllocationType ALLOCATION_TYPE_FLOAT(6);
static constexpr AllocationType ALLOCATION_TYPE_DOUBLE(7);
static constexpr AllocationType ALLOCATION_TYPE_BITMAP(8);

#endif //RUNTIME_ALLOCATIONTYPE_H
