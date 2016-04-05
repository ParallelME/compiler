#include "Buffer.hpp"
#include "Runtime.hpp"
#include <algorithm>
#include <cstdlib>

Buffer::Buffer(Device &device, Flags flags, size_t size)
        : _buffer{nullptr}, _device(device), _size(size) {
    cl_mem_flags clFlags;

    switch(flags) {
        case ReadOnly: clFlags = CL_MEM_READ_ONLY; break;
        case ReadWrite: clFlags = CL_MEM_READ_WRITE; break;
        case WriteOnly: clFlags = CL_MEM_WRITE_ONLY; break;
    }

    _device.clContext([&] (cl_context context) {
        int err;
        _buffer = clCreateBuffer(context, clFlags, size, NULL, &err);
        if(err < 0)
            throw BufferConstructionError(std::to_string(err));
    });
}

Buffer::~Buffer() {
    if(_buffer)
        clReleaseMemObject(_buffer);
}

void Buffer::copyFrom(void *host, size_t size) {
    std::unique_lock<std::mutex> lock(_mutex);
    _device.clQueue([&] (cl_command_queue queue) {
        int err;

        void *data = clEnqueueMapBuffer(queue, _buffer, CL_TRUE, CL_MAP_WRITE,
                0, size, 0, NULL, NULL, &err);
        if(err < 0)
        throw BufferMapError(std::to_string(err));

        memcpy(data, host, std::min(_size, size));

        clEnqueueUnmapMemObject(queue, _buffer, data, 0, NULL, NULL);
    });
}

void Buffer::copyTo(void *host, size_t size) {
    std::unique_lock<std::mutex> lock(_mutex);
    _device.clQueue([&] (cl_command_queue queue) {
        int err;

        void *data = clEnqueueMapBuffer(queue, _buffer, CL_TRUE, CL_MAP_READ, 0,
                size, 0, NULL, NULL, &err);
        if(err < 0)
            throw BufferMapError(std::to_string(err));

        memcpy(host, data, std::min(_size, size));
        clEnqueueUnmapMemObject(queue, _buffer, data, 0, NULL, NULL);

        clFinish(queue);
        if(err < 0)
            throw BufferMapError(std::to_string(err));
    });
}

