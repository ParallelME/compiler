#ifndef BUFFER_HPP
#define BUFFER_HPP

#include <cstdlib>
#include <stdexcept>
#include <jni.h>

class Device;

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
 * This class is an abstract class that defines the interface of a buffer that
 * should be implemented for a given programming platform.
 * Instances of implementations of this class can be created by calling
 * device.createBuffer().
 */
class Buffer {

public:
    Buffer(const Buffer &) = delete;
    Buffer &operator=(const Buffer &) = delete;

    virtual ~Buffer() { }

    /**
     * Types that can be used on a buffer.
     */
    enum ElementType {
        CHAR, CHAR2, CHAR4, UCHAR, UCHAR2, UCHAR4, RGBA,
        SHORT, SHORT2, SHORT4, USHORT, USHORT2, USHORT4,
        INT, INT2, INT4, UINT, UINT2, UINT4,
        FLOAT, FLOAT2, FLOAT4
    };

    /**
     * Array that stores the size in bytes of each element.
     */
    const size_t ElementSize[22] = {
        1, 2, 4, 1, 2, 4, 4,   // char, uchar and rgba
        2, 4, 8, 2, 4, 8,   // short and ushort
        4, 8, 16, 4, 8, 16, // int and uint
        4, 8, 16            // float
    };

    /// Copies from an input array of bytes.
    virtual void copyFrom(jbyteArray array) = 0;

    /// Copies from an input array of shorts.
    virtual void copyFrom(jshortArray array) = 0;

    /// Copies from an input array of ints.
    virtual void copyFrom(jintArray array) = 0;

    /// Copies from an input array of floats.
    virtual void copyFrom(jfloatArray array) = 0;

    /// Copies from an input Bitmap.
    virtual void copyFrom(jobject bitmap) = 0;

    /// Copies to an output array of bytes.
    virtual void copyTo(jbyteArray array) = 0;

    /// Copies to an output array of shorts.
    virtual void copyTo(jshortArray array) = 0;

    /// Copies to an output array of ints.
    virtual void copyTo(jintArray array) = 0;

    /// Copies to an output array of floats.
    virtual void copyTo(jfloatArray array) = 0;

    /// Copies to an output Bitmap object.
    virtual void copyTo(jobject bitmap) = 0;

    /**
     * Returns the device where the buffer's memory is.
     */
    inline Device *device() {
         return _device;
    }

    /**
     * Returns the type of the elements of the buffer.
     */
    inline ElementType type() {
         return _type;
    }

    /**
     * Returns the number of elements of the X dimension of the buffer.
     */
    inline size_t numX() {
         return _numX;
    }

    /**
     * Returns the number of elements of the Y dimension of the buffer.
     */
    inline size_t numY() {
        return _numY;
    }

    /**
     * Returns the number of elements of the Z dimension of the buffer.
     */
    inline size_t numZ() {
        return _numZ;
    }

    /**
     * Returns the size of the buffer (the size of the element times how
     * many elements).
     */
    inline size_t size() {
        return ElementSize[_type] * _numX * _numY * _numZ;
    }

protected:
    /**
     * Saves some information that is useful to the derived classes.
     */
    Buffer(Device *device, ElementType type, int numX, int numY, int numZ)
        : _device{device}, _type(type), _numX(numX), _numY(numY), _numZ(numZ) {  }

private:
    Device *_device;    /// Device of this buffer.
    ElementType _type;  /// Type of the elements in the buffer.
    int _numX;          /// Number of elements in the X dimension of buffer.
    int _numY;          /// Number of elements in the Y dimension of buffer.
    int _numZ;          /// Number of elements in the Z dimension of buffer.
};

#endif // !BUFFER_HPP
