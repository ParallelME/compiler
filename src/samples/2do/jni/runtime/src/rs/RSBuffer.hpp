#ifndef RS_RSBUFFER_HPP
#define RS_RSBUFFER_HPP

#include <Buffer.hpp>
#include <jni.h>

class RSBuffer : public Buffer {
    jobject _allocation;
    jclass _allocationC;

    /// General copyFrom/copyTo, expected the correct methodID.
    void copy(jmethodID copyMethod, void *host);

    /// Returns a local reference to the method given.
    jmethodID getAllocMethod(const char *name, const char *sig);

public:
    /**
     * Constructs the buffer, an interface to a RenderScript Allocation.
     */
    RSBuffer(Device *device, Buffer::ElementType type, int numX, int numY = 1,
            int numZ = 1);

    ~RSBuffer();

    /**
     * Table to convert from ElementType to the name of the Element.* function
     * that returns the correspondent element.
     * This MUST be updated together with ElementType.
     */
    const char *elemTypeConv[22] {
        "I8", "I8_2", "I8_4", "U8", "U8_2", "U8_4", "RGBA_8888",
        "I16", "I16_2", "I16_4", "U16", "U16_2", "U16_4",
        "I32", "I32_2", "I32_4", "U32", "U32_2", "U32_4",
        "F32", "F32_2", "F32_4"
    };

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

    /// Returns the allocation of the buffer.
    inline jobject allocation() {
         return _allocation;
    }
};

#endif // !RS_RSBUFFER_HPP
