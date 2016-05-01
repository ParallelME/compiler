#ifndef KERNEL_HPP
#define KERNEL_HPP

#include "Buffer.hpp"
#include "Program.hpp"
#include <clLoader.h>
#include <atomic>
#include <memory>
#include <mutex>
#include <vector>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include "../error.h"
#include "ExtraArgument.hpp"

/**
 * Exception thrown if a kernel failed to be created on all platforms when being
 * constructed.
 * The error message can be accessed through the what() function.
 */
class KernelConstructionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if a kernel failed to set one argument.
 * The error message can be accessed through the what() function.
 */
class KernelArgumentError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * This class represents a kernel from a Program.
 */
class Kernel {
    std::unordered_map<unsigned, std::shared_ptr<Buffer> > _buffers;
    std::vector<cl_kernel> _clKernels;
    std::mutex _mutex;
    size_t _offset;
    size_t _workSize;

public:
    /**
     * Constructs a kernel.
     */
    Kernel(const char *name, Program &program);

    ~Kernel();

    /**
     * Sets the range of execution.
     */
    inline Kernel *setWorkRange(size_t offset, size_t workSize) {
        std::unique_lock<std::mutex> lock(_mutex);
        _offset = offset;
        _workSize = workSize;

        return this;
    }

    /**
     * Sets a buffer as the argument to the kernel, only to the device of the
     * buffer.
     */
    Kernel *setArg(unsigned argIndex, std::shared_ptr<Buffer> buffer) {
        std::unique_lock<std::mutex> lock(_mutex);
        Device &device = buffer->device();

        buffer->clBuffer([&] (cl_mem clBuffer) {
            int err = clSetKernelArg(_clKernels[device.id()], argIndex,
                    sizeof(clBuffer), &clBuffer);
            if(err < 0)
                throw KernelArgumentError(std::to_string(err));
        });

        auto it = _buffers.find(argIndex);
        if(it != _buffers.end())
            _buffers.erase(it); // New buffer took its place.

        _buffers.emplace(argIndex, buffer);
        return this;
    }


    /**
     * Sets a primitive type argument to the kernel.
     * This function doesn't work with arrays/vector types, only scalars.
     * The argIndex parameter is the position of the parameter in the kernel.
     */
    template<typename T>
    Kernel *setArg(unsigned argIndex, T primitive) {
        std::unique_lock<std::mutex> lock(_mutex);
        int err;

        for(auto &kernel : _clKernels) {
            err = clSetKernelArg(kernel, argIndex, sizeof(primitive), &primitive);
            if(err < 0)
                throw KernelArgumentError(std::to_string(err));
        }

        auto it = _buffers.find(argIndex);
        if(it != _buffers.end())
            _buffers.erase(it); // argIndex is not a buffer anymore.

        return this;
    }

    Kernel *setArg(unsigned argIndex, ExtraArgument extraArgument){
        switch(extraArgument.argType){
            case (ArgType::FLOAT):
                printError("DEBUG: Extra Argument is Float");
                printError("DEBUG: Extra Argument value %f", extraArgument.value.f);
                return setArg(argIndex, extraArgument.value.f);
            case (ArgType::CHAR):
                return setArg(argIndex, extraArgument.value.c);
            case (ArgType::INT):
                printError("DEBUG: Extra Argument is Int");
                printError("DEBUG: Extra Argument value %d", extraArgument.value.i);
                return setArg(argIndex, extraArgument.value.i);
            case (ArgType::UCHAR):
                return setArg(argIndex, extraArgument.value.uc);
            case (ArgType::SHORT):
                return setArg(argIndex, extraArgument.value.s);
        }
    }


    /**
     * Returns the buffer with the given argIndex or nullptr if a buffer wasn't
     * found.
     */
    inline Buffer *buffer(unsigned argIndex) {
        auto it = _buffers.find(argIndex);
        if(it != _buffers.end())
            return it->second.get();
        else
            return nullptr;
    }

    /**
     * This function calls fn with the kernel as parameter. It guarantees that
     * only one thread will be accessing the kernel at the same time.
     */
    inline void clKernel(unsigned id, std::function<void (cl_kernel)> fn) {
        std::unique_lock<std::mutex> lock(_mutex);
        fn(_clKernels[id]);
    }

    /// Returns the offset of the kernel work range.
    inline size_t offset() {
        std::unique_lock<std::mutex> lock(_mutex);
        return _offset;
    }

    /// Returns the work size of the kernel work range.
    inline size_t workSize() {
        std::unique_lock<std::mutex> lock(_mutex);
        return _workSize;
    }
};

#endif // !KERNEL_HPP
