//
// Created by pedro on 4/9/16.
//
#include "Allocation.hpp"

Allocation::Allocation(jbooleanArray pointer, AllocationType type, int elements){
    this->booleanPointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

Allocation::Allocation(jbyteArray pointer, AllocationType type, int elements){
    this->bytePointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

Allocation::Allocation(jcharArray pointer, AllocationType type, int elements){
    this->charPointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

Allocation::Allocation(jshortArray pointer, AllocationType type, int elements){
    this->shortPointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

Allocation::Allocation(jintArray pointer, AllocationType type, int elements){
    this->intPointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

Allocation::Allocation(jlongArray pointer, AllocationType type, int elements){
    this->longPointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

Allocation::Allocation(jfloatArray pointer, AllocationType type, int elements){
    this->floatPointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

Allocation::Allocation(jdoubleArray pointer, AllocationType type, int elements){
    this->doublePointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

Allocation::Allocation(jobject pointer, AllocationType type, int elements){
    this->bitmapPointer = pointer;
    this->type = type;
    this->elements = elements;
    this->size = elements * type.size();
    this->deleted = false;
    printError("DEBUG: Creating Allocation of %d size", this->size);
}

jbooleanArray Allocation::getBooleanPointer(){
    return this->booleanPointer;
}

jbyteArray Allocation::getBytePointer(){
    return this->bytePointer;
}

jcharArray Allocation::getCharPointer(){
    return this->charPointer;
}

jshortArray Allocation::getShortPointer(){
    return this->shortPointer;
}

jintArray Allocation::getIntPointer(){
    return this->intPointer;
}

jlongArray Allocation::getLongPointer(){
    return this->longPointer;
}

jdoubleArray Allocation::getDoublePointer(){
    return this->doublePointer;
}

jfloatArray Allocation::getFloatPointer(){
    return this->floatPointer;
}

jobject Allocation::getBitmapPointer(){
    return this->bitmapPointer;
}



AllocationType Allocation::getType(){
    return this->type;
}

int Allocation::getElements(){
    return this->elements;
}

int Allocation::getSize(){
    return this->size;
}

void Allocation::deleteReference(){
    this->deleted = true;
}

bool Allocation::isDeleted(){
    return this->deleted;
}




