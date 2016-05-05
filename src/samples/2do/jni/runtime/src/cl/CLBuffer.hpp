#ifndef CL_CLBUFFER_HPP
#define CL_CLBUFFER_HPP

#include "dynloader/dynLoader.h"
#include <Buffer.hpp>

class CLBuffer : public Buffer {
    cl_mem _mem;

    /// Copies from a simple pointer.
    void copyFromPtr(void *host);

    /// Copies to a simple pointer.
    void copyToPtr(void *host);

    /// Copies from an jarray.
    void copyFromArray(jarray array);

    /// Copies to an jarray.
    void copyToArray(jarray array);

public:
    /**
     * Constructs the buffer.
     * @param device The device where the buffer will run at.
     * @param flags The flags that specify how the buffer is allocated. See
     * AccessFlags.
     * @param type The type of the buffer elements.
     * @param numX The number of buffer elements in the X dimension.
     * @param numY The number of buffer elements in the Y dimension.
     * @param numZ The number of buffer elements in the Z dimension.
     */
    CLBuffer(Device *device, Buffer::ElementType type, int numX, int numY = 1,
            int numZ = 1);

    ~CLBuffer();

    void copyFrom(jbyteArray array);
    void copyFrom(jshortArray array);
    void copyFrom(jintArray array);
    void copyFrom(jfloatArray array);
    void copyFrom(jobject bitmap);
    void copyTo(jbyteArray array);
    void copyTo(jshortArray array);
    void copyTo(jintArray array);
    void copyTo(jfloatArray array);
    void copyTo(jobject bitmap);

    /// Returns the cl_mem object of the buffer.
    inline cl_mem clMem() {
        return _mem;
    }
};

#endif // !CL_CLBUFFER_HPP
