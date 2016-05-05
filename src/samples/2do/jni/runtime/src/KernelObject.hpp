#ifndef KERNELOBJECT_HPP
#define KERNELOBJECT_HPP

#include <cstdlib>
#include <memory>
#include <Device.hpp>
#include <Kernel.hpp>

class Buffer;

/**
 * This abstract class represents a kernel object of a given runtime platform.
 */
class KernelObject {

protected:
    std::shared_ptr<Device> _device;

    KernelObject(std::shared_ptr<Device> device) : _device(device) { }

public:
    KernelObject(const KernelObject &) = delete;
    KernelObject &operator=(const KernelObject &) = delete;
    virtual ~KernelObject() { }

    /**
     * Returns the device of this KernelObject.
     */
    inline Device *device() {
         return _device.get();
    }

    /**
     * Runs the KernelObject on the target device.
     */
    virtual void run(int xDim, int yDim, int zDim) = 0;

    /**
     * Sets the input buffer.
     */
    virtual void setInputBuffer(std::shared_ptr<Buffer> buffer) = 0;

    /**
     * Sets the output buffer.
     */
    virtual void setOutputBuffer(std::shared_ptr<Buffer> buffer) = 0;

    virtual void setArg(const std::string &name, std::shared_ptr<Buffer> buffer) = 0;
    virtual void setArg(const std::string &name, jchar primitive) = 0;
    virtual void setArg(const std::string &name, jbyte primitive) = 0;
    virtual void setArg(const std::string &name, jshort primitive) = 0;
    virtual void setArg(const std::string &name, jint primitive) = 0;
    virtual void setArg(const std::string &name, jfloat primitive) = 0;
};

#endif // !KERNELOBJECT_HPP
