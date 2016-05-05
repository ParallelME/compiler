#include "RSBuffer.hpp"
#include "RSDevice.hpp"

RSBuffer::RSBuffer(Device *dev, Buffer::ElementType type, int numX, int numY,
        int numZ) : Buffer(dev, type, numX, numY, numZ) {
    auto rsDev = (RSDevice *) dev;
    auto env = rsDev->jenv();

    auto elem = rsDev->newElement(env, elemTypeConv[type]);
    auto rsType = rsDev->newType(env, elem, numX, numY, numZ);
    auto localAlloc = rsDev->newAllocation(env, rsType);
    _allocation = env->NewGlobalRef(localAlloc);
    if(!_allocation)
        throw std::runtime_error("Failed trying to create a global ref.");

    auto localAllocC = rsDev->allocationClass();
    _allocationC = (jclass) env->NewGlobalRef(localAllocC);
    if(!_allocationC)
        throw std::runtime_error("Failed trying to get a new global ref.");

    env->DeleteLocalRef(elem);
    env->DeleteLocalRef(rsType);
    env->DeleteLocalRef(localAlloc);
}

RSBuffer::~RSBuffer() {
    auto env = device()->jenv();

    env->DeleteGlobalRef(_allocation);
    _allocation = nullptr;
    env->DeleteGlobalRef(_allocationC);
    _allocationC = nullptr;
}

#include <chrono>
#include "../util/error.h"
void RSBuffer::copy(jmethodID copyMethod, void *host) {
    auto env = device()->jenv();

printError("copy");
auto start = std::chrono::steady_clock::now();
    env->CallVoidMethod(_allocation, copyMethod, host);
    if(env->ExceptionCheck())
        throw std::runtime_error("Failed to call copyFrom/copyTo in RenderScript.");
auto end = std::chrono::steady_clock::now();
printError("copy2 - %.2lfms", std::chrono::duration <double, std::milli> (end - start).count());
}

jmethodID RSBuffer::getAllocMethod(const char *name, const char *sig) {
    auto env = device()->jenv();

printError("func");
auto start = std::chrono::steady_clock::now();
    auto func = env->GetMethodID(_allocationC, name, sig);
    if(!func)
        throw std::runtime_error("Failed to get the Allocation method.");

auto end = std::chrono::steady_clock::now();
printError("func2 - %.2lfms", std::chrono::duration <double, std::milli> (end - start).count());
    return func;
}

void RSBuffer::copyFrom(jbyteArray array) {
    copy(getAllocMethod("copyFrom", "([B)V"), array);
}

void RSBuffer::copyFrom(jshortArray array) {
    copy(getAllocMethod("copyFrom", "([S)V"), array);
}

void RSBuffer::copyFrom(jintArray array) {
    copy(getAllocMethod("copyFrom", "([I)V"), array);
}

void RSBuffer::copyFrom(jfloatArray array) {
    copy(getAllocMethod("copyFrom", "([F)V"), array);
}

void RSBuffer::copyFrom(jobject bitmap) {
    copy(getAllocMethod("copyFrom", "(Landroid/graphics/Bitmap;)V"), bitmap);
}

void RSBuffer::copyTo(jbyteArray array) {
    copy(getAllocMethod("copyTo", "([B)V"), array);
}

void RSBuffer::copyTo(jshortArray array) {
    copy(getAllocMethod("copyTo", "([S)V"), array);
}

void RSBuffer::copyTo(jintArray array) {
    copy(getAllocMethod("copyTo", "([I)V"), array);
}

void RSBuffer::copyTo(jfloatArray array) {
    copy(getAllocMethod("copyTo", "([F)V"), array);
}

void RSBuffer::copyTo(jobject bitmap) {
    copy(getAllocMethod("copyTo", "(Landroid/graphics/Bitmap;)V"), bitmap);
}

