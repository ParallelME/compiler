#include "CLBuffer.hpp"
#include "CLDevice.hpp"
#include <algorithm>
#include <string>
#include <android/bitmap.h>

CLBuffer::CLBuffer(Device *device, Buffer::ElementType type, int numX, int numY,
        int numZ)
        : Buffer(device, type, numX, numY, numZ) {
    auto clDev = (CLDevice *) device;
    int err;
    cl_mem_flags clFlags = CL_MEM_READ_WRITE; // For compatibility with RS.

    _mem = clCreateBuffer(clDev->clContext(), clFlags, size(), nullptr, &err);
    if(err < 0)
        throw BufferConstructionError(std::to_string(err));
}

CLBuffer::~CLBuffer() {
    if(_mem)
        clReleaseMemObject(_mem);
}

void CLBuffer::copyFromPtr(void *host) {
    int err;
    auto clDev = (CLDevice *) device();

    void *data = clEnqueueMapBuffer(clDev->clQueue(), _mem, CL_TRUE, CL_MAP_WRITE,
            0, size(), 0, nullptr, nullptr, &err);
    if(err < 0)
        throw BufferCopyError(std::to_string(err));

    memcpy(data, host, size());
    clEnqueueUnmapMemObject(clDev->clQueue(), _mem, data, 0, nullptr, nullptr);

    err = clFinish(clDev->clQueue());
    if(err < 0)
        throw BufferCopyError(std::to_string(err));
}

void CLBuffer::copyToPtr(void *host) {
    int err;
    auto clDev = (CLDevice *) device();

    void *data = clEnqueueMapBuffer(clDev->clQueue(), _mem, CL_TRUE, CL_MAP_READ,
            0, size(), 0, nullptr, nullptr, &err);
    if(err < 0)
        throw BufferCopyError(std::to_string(err));

    memcpy(host, data, size());
    clEnqueueUnmapMemObject(clDev->clQueue(), _mem, data, 0, nullptr, nullptr);

    err = clFinish(clDev->clQueue());
    if(err < 0)
        throw BufferCopyError(std::to_string(err));
}

void CLBuffer::copyFromArray(jarray array) {
    auto env = device()->jenv();
    auto data = env->GetPrimitiveArrayCritical(array, nullptr);
    if(!data)
        throw BufferCopyError("Failed to get primitive array ptr.");

    copyFromPtr(data);

    env->ReleasePrimitiveArrayCritical(array, data, 0);
}

void CLBuffer::copyToArray(jarray array) {
    auto env = device()->jenv();
    auto data = env->GetPrimitiveArrayCritical(array, nullptr);
    if(!data)
        throw BufferCopyError("Failed to get primitive array ptr.");

    copyToPtr(data);

    env->ReleasePrimitiveArrayCritical(array, data, 0);
}

void CLBuffer::copyFrom(jbyteArray array) {
    copyFromArray(array);
}

void CLBuffer::copyFrom(jshortArray array) {
    copyFromArray(array);
}

void CLBuffer::copyFrom(jintArray array) {
    copyFromArray(array);
}

void CLBuffer::copyFrom(jfloatArray array) {
    copyFromArray(array);
}

void CLBuffer::copyFrom(jobject bitmap) {
    auto env = device()->jenv();

    void *out;
    int ret = AndroidBitmap_lockPixels(env, bitmap, &out);
    if(ret < 0)
        throw std::runtime_error("Failed to lock bitmap.");

    copyFromPtr(out);

    AndroidBitmap_unlockPixels(env, bitmap);
    out = nullptr;
}

void CLBuffer::copyTo(jbyteArray array) {
    copyToArray(array);
}

void CLBuffer::copyTo(jshortArray array) {
    copyToArray(array);
}

void CLBuffer::copyTo(jintArray array) {
    copyToArray(array);
}

void CLBuffer::copyTo(jfloatArray array) {
    copyToArray(array);
}

void CLBuffer::copyTo(jobject bitmap) {
    auto env = device()->jenv();

    void *out;
    int ret = AndroidBitmap_lockPixels(env, bitmap, &out);
    if(ret < 0)
        throw std::runtime_error("Failed to lock bitmap.");

    copyToPtr(out);

    AndroidBitmap_unlockPixels(env, bitmap);
    out = nullptr;
}

