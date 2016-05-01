//
// Created by pedro on 4/6/16.
//

#ifndef RUNTIME_ALLOCATION_H
#define RUNTIME_ALLOCATION_H

#include "AllocationType.hpp"
#include <jni.h>
#include "../error.h"

class Allocation{
private:
    jbooleanArray booleanPointer;
    jbyteArray bytePointer;
    jcharArray charPointer;
    jshortArray shortPointer;
    jintArray intPointer;
    jlongArray longPointer;
    jfloatArray floatPointer;
    jdoubleArray doublePointer;
    jobject bitmapPointer;

    AllocationType type;
    int elements; //size should be elements * sizeof(type)
    int size;
    bool deleted;

public:
    Allocation(jbooleanArray pointer, AllocationType type, int elements);
    Allocation(jbyteArray pointer, AllocationType type, int elements);
    Allocation(jcharArray pointer, AllocationType type, int elements);
    Allocation(jshortArray pointer, AllocationType type, int elements);
    Allocation(jintArray pointer, AllocationType type, int elements);
    Allocation(jlongArray pointer, AllocationType type, int elements);
    Allocation(jfloatArray pointer, AllocationType type, int elements);
    Allocation(jdoubleArray pointer, AllocationType type, int elements);
    Allocation(jobject pointer, AllocationType type, int elements);

    jbooleanArray getBooleanPointer();
    jbyteArray getBytePointer();
    jcharArray getCharPointer();
    jshortArray getShortPointer();
    jintArray getIntPointer();
    jlongArray getLongPointer();
    jfloatArray getFloatPointer();
    jdoubleArray getDoublePointer();
    jobject getBitmapPointer();

    AllocationType getType();

    int getElements();

    int getSize();

    void deleteReference();

    bool isDeleted();

};

#endif //RUNTIME_ALLOCATION_H
