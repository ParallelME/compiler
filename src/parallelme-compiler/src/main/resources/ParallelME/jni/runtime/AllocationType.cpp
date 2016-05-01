//
// Created by pedro on 4/7/16.
//

#include "AllocationType.hpp"

int AllocationType::size() const{
    switch(index){
        case ALLOCATION_TYPE_BOOLEAN: //I probably can't create a bit so I'm giving it a byte;
            return 1;
        case ALLOCATION_TYPE_BYTE:
            return 1;
        case ALLOCATION_TYPE_CHAR: //Java char have 2 bytes
            return 2;
        case ALLOCATION_TYPE_SHORT:
            return 2;
        case ALLOCATION_TYPE_INT:
            return 4;
        case ALLOCATION_TYPE_LONG:
            return 8;
        case ALLOCATION_TYPE_FLOAT:
            return 4;
        case ALLOCATION_TYPE_DOUBLE:
            return 8;
        case ALLOCATION_TYPE_BITMAP:
            return 4;
        default:
            //ERRO
            return 0;
    }
}