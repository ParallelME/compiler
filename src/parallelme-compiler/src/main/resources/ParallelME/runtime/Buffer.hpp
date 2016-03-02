#ifndef BUFFER_HPP
#define BUFFER_HPP

#include "Device.hpp"
#include <clLoader.h>
#include <cstdlib>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>

/**
 * Exception thrown if the buffer wasn't created successfully.
 * The error message can be accessed through the what() function.
 */
class BufferConstructionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if the buffer wasn't mapped to host successfully.
 * The error message can be accessed through the what() function.
 */
class BufferMapError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * This class represents a Buffer used in a Task.
 * It is a random-access iterator type.
 */
class Buffer {
    cl_mem _buffer;
    Device &_device;
    size_t _size;
    std::mutex _mutex;

public:
    /// Equivalent to part of the cl_mem_flags.
    enum Flags {
        ReadOnly,
        ReadWrite,
        WriteOnly
    };

    /**
     * Constructs the buffer.
     * @param device The device where the buffer will run at.
     * @param flags The flags that specify how the buffer is allocated. See
     * AccessFlags.
     * @param size The size in bytes of the memory region.
     */
    Buffer(Device &device, Flags flags, size_t size);

    ~Buffer();

    /**
     * Copies size bytes from the host data to the buffer.
     * If size is bigger than the one used when creating the buffer, only the
      * buffer size will be copied.
     */
    void copyFrom(void *host, size_t size);

    /**
     * Copies size bytes from the buffer to the given host pointer.
     * If size is bigger than the one used when creating the buffer, only the
      * buffer size will be copied.
     */
    void copyTo(void *host, size_t size);

    /**
     * Returns the reference to the device of this buffer.
     */
    inline Device &device() {
        return _device;
    }

    /**
     * This function calls fn with the buffer as parameter. It guarantees that
     * only one thread will be accessing the buffer at the same time.
     */
    inline void clBuffer(std::function<void (cl_mem)> fn) {
        std::unique_lock<std::mutex> lock(_mutex);
        fn(_buffer);
    }
};

#endif // !BUFFER_HPP
