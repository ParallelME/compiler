#ifndef KERNEL_HPP
#define KERNEL_HPP

#include <cstdlib>
#include <unordered_map>
#include <string>
#include <stdexcept>
#include "Buffer.hpp"
#include "Device.hpp"
#include "Program.hpp"

class KernelObject;

/// Type of a KernelObject pair of unique ptrs.
typedef std::pair< DeviceID, std::unique_ptr<KernelObject> >
    KernelObjectPair;

/// Type of a KernelObject hash of unique ptrs.
typedef std::map< DeviceID, std::unique_ptr<KernelObject> >
    KernelObjectMap;

/**
 * Exception thrown if the kernel failed execute.
 * The error message can be accessed through the what() function.
 */
class KernelExecutionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if the kernel failed to be created.
 * The error message can be accessed through the what() function.
 */
class KernelConstructionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if the kernel failed to configure the argument.
 * The error message can be accessed through the what() function.
 */
class KernelArgError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};
/**
 * This class represents a Task's kernel.
 */
class Kernel {

public:
    Kernel(const Kernel &) = delete;
    Kernel &operator=(const Kernel &) = delete;

    ~Kernel();

    /**
     * Sets the range of execution.
     * @param xDim Number of elements in the x dimension.
     * @param yDim Number of elements in the y dimension.
     * @param zDim Number of elements in the z dimension.
     */
    inline Kernel *setWorkRange(int xDim, int yDim = 1, int zDim = 1) {
        _xDim = xDim;
        _yDim = yDim;
        _zDim = zDim;

        return this;
    }

    /**
     * Sets the input buffer to the kernel, only to the device of the buffer.
     * The input buffer on OpenCL is always the first parameter of the kernel.
     * Note that you can make this buffer readwrite on RenderScript by using
     * the old prototype format and using pointers to access it.
     * If the xDim, yDim and zDim weren't set previously, the dimensions of this
     * buffer are used as the dimensions of the kernel.
     */
    Kernel *setInputBuffer(std::shared_ptr<Buffer> buffer);

    /**
     * Sets the output buffer to the kernel, only to the device of the buffer.
     * The output buffer on OpenCL is always the second parameter of the kernel.
     * Note that you can make this buffer readwrite on RenderScript by using
     * the old prototype format and using pointers to access it.
     */
    Kernel *setOutputBuffer(std::shared_ptr<Buffer> buffer);

    /**
     * Sets an arbitrary buffer as a parameter, except for the first two,
     * only to the device of the buffer.
     */
    Kernel *setArg(const std::string &name, std::shared_ptr<Buffer> buffer);

    /// Sets a primitive char to the kernel.
    Kernel *setArg(const std::string &name, jchar primitive);

    /// Sets a primitive byte to the kernel.
    Kernel *setArg(const std::string &name, jbyte primitive);

    /// Sets a primitive short to the kernel.
    Kernel *setArg(const std::string &name, jshort primitive);

    /// Sets a primitive int to the kernel.
    Kernel *setArg(const std::string &name, jint primitive);

    /// Sets a primitive float to the kernel.
    Kernel *setArg(const std::string &name, jfloat primitive);

    /**
     * Returns the input buffer or nullptr if it wasn't set.
     */
    inline Buffer *inputBuffer() {
         return _inputBuffer.get();
    }

    /**
     * Returns the output buffer or nullptr if it wasn't set.
     */
    inline Buffer *outputBuffer() {
        return _outputBuffer.get();
    }

    /// Returns the x dimension of the kernel work range.
    inline size_t xDim() {
        return _xDim;
    }

    /// Returns the y dimension of the kernel work range.
    inline size_t yDim() {
        return _yDim;
    }

    /// Returns the z dimension of the kernel work range.
    inline size_t zDim() {
        return _zDim;
    }

    /**
     * Constructs the Kernel object. Only the Task class should do it.
     * The proper way of creating a kernel is through Task::addKernel().
     * @param name The kernel's name.
     * @param programObjects The program objects that the Task supports.
     * @see Task::addKernel
     */
    Kernel(const std::string &name, const ProgramObjectMap &programObjects);

    /// Returns the map of kernel objects.
    inline KernelObjectMap &objects() {
        return _kernelObjects;
    }

private:
    int _xDim;
    int _yDim;
    int _zDim;
    KernelObjectMap _kernelObjects;
    std::shared_ptr<Buffer> _inputBuffer, _outputBuffer;
};

#endif // !KERNEL_HPP
