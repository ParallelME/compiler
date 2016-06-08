/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_BUFFER_HPP
#define PARALLELME_BUFFER_HPP

#include <cstdlib>
#include <memory>
#include <stdexcept>
#include <jni.h>

struct _cl_mem;

namespace parallelme {
class Device;
class Worker;
class Kernel;

/**
 * Exception thrown if the buffer failed to construct and initialize.
 * The error message can be accessed through the what() function.
 */
class BufferConstructionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if the buffer failed to copy data to or from the device.
 * The error message can be accessed through the what() function.
 */
class BufferCopyError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if a copyTo*() function was called and the buffer didn't
 * have any data saved as a reference or inside the device's buffer.
 * The error message can be accessed through the what() function.
 */
class BufferEmptyError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};


/**
 * This class represents a buffer that can be used to send and receive data
 * to/from the kernels.
 * Please pay attention to the saveCopyFrom functions: they don't make a
 * copy on the spot, instead saving a reference to the array, buffer or pointer
 * and only copying right before execution.
 *
 * @author Renato Utsch
 */
class Buffer {

public:
    /**
     * Creates a memory buffer.
     * @param size The size in bytes of the buffer.
     */
    Buffer(size_t size);

    Buffer(const Buffer &) = delete;
    Buffer &operator=(const Buffer &) = delete;

    ~Buffer();

    /**
     * Helper types that can be used when specifying the size of the buffer when
     * coming from java.
     */
    enum Type {
        BYTE, BYTE2, BYTE4, RGBA,
        CHAR, CHAR2, CHAR4, SHORT, SHORT2, SHORT4,
        INT, INT2, INT4, FLOAT, FLOAT2, FLOAT4
    };

    /**
     * Helper function that returns the size of a buffer with the given type and
     * number of elements.
     */
    static inline size_t sizeGenerator(size_t numElements, Type type) {
        /// Array that stores the size in bytes of each java type.
        static const size_t TypeSizes[16] = {
            1, 2, 4, 4,         // byte, byte2, byte4 and rgba
            2, 4, 8, 2, 4, 8,   // char and short
            4, 8, 16, 4, 8, 16  // int and float
        };

        return numElements * TypeSizes[type];
    }


    /**
     * Saves a global reference to a jarray. This reference will be used later
     * as the data source for the Buffer, supplying the contents that will be
     * copied to the buffer before a kernel or copyTo* function executes.
     * Because of this, the jarray must be kept alive until the kernel executes
     * or the copyTo* function is called (in case the kernel is not executed).
     * If there is already another source, it will be discarded and this will
     * be set as the new source.
     */
    void setJArraySource(JNIEnv *env, jarray array);

    /**
     * Saves a global reference to an android bitmap . This reference will be
     * used later as the data source for the Buffer, supplying the contents that
     * will be copied to the buffer before a kernel or copyTo* function
     * executes.
     * Because of this, the jarray must be kept alive until the kernel executes
     * or the copyTo* function is called (in case the kernel is not executed).
     * If there is already another source, it will be discarded and this will
     * be set as the new source.
     */
    void setAndroidBitmapSource(JNIEnv *env, jobject bitmap);

    /**
     * Saves the host pointer to copy  size() bytes to the internal memory.
     * Important: this function only saves the pointer to where the data is,
     * and only actually makes the copy when the buffer is being set as the
     * argument to a kernel or being copied back.
     * If there is another source already, it will be discarded and this will
     * be the new source.
     */
    void setSource(void *host);

    /**
     * Helper method to copy size() bytes to a jarray. Throws
     * EmptyBufferError if the buffer doesn't have any data.
     */
    void copyToJArray(JNIEnv *env, jarray array);

    /**
     * Helper method to copy size() bytes to an Android bitmap. Throws
     * EmptyBufferError if the buffer doesn't have any data.
     */
    void copyToAndroidBitmap(JNIEnv *env, jobject bitmap);

    /**
     * Copies size() bytes to an host pointer. Throws
     * EmptyBufferError if the buffer doesn't have any data.
     */
    void copyTo(void *host);

    /**
     * Returns the size of the buffer in bytes.
     */
    inline size_t size() {
        return _size;
    }

private:
    friend class Kernel;

    /**
     * Returns the OpenCL memory object. Creates it if it wasn't created yet.
     * This should only be called by the Kernel class.
     */
    _cl_mem *clMem(std::shared_ptr<Device> device);

    /**
     * Creates an OpenCL memory object on the given device or makes a copy to
     * a memory object on the new device if one already exists and the flag
     * is set.
     */
    void createMemoryObject(std::shared_ptr<Device> newDevice, bool copyOld);

    /**
     * Release the copy structures before creating a new copy source.
     */
    void releaseCopySources(JNIEnv *env);

    /**
     * If has copy sources.
     */
    inline bool hasCopySource() {
        return _copyPtr || _copyArray || _copyBitmap;
    }

    /**
     * Properly makes the copies to the _mem structure.
     */
    void makeCopy(JNIEnv *env);

    /**
     * Properly makes the copy from the pointer to the _mem structure.
     */
    void makeCopyFrom(void *host);

    size_t _size;                       /// Size of the buffer.
    _cl_mem *_mem;                      /// Pointer to the buffer.
    std::shared_ptr<Device> _device;    /// Device of the buffer.
    void *_copyPtr;                     /// Pointer with the data to be copied.
    jarray _copyArray;                  /// Array to be copied.
    jobject _copyBitmap;                /// Bitmap to be copied.
};

}

#endif // !PARALLELME_BUFFER_HPP
