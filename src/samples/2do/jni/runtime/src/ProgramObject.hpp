#ifndef PROGRAMOBJECT_HPP
#define PROGRAMOBJECT_HPP

#include <memory>

class Device;
class KernelObject;

/**
 * This is an abstract class that represents the compiled source code for a
 * given device. It is implemented for each different platform.
 */
class ProgramObject {

protected:
    std::shared_ptr<Device> _device;

    ProgramObject(std::shared_ptr<Device> device) : _device(device) { }

public:
    ProgramObject(const ProgramObject &) = delete;
    ProgramObject &operator=(const ProgramObject &) = delete;
    virtual ~ProgramObject() { }

    /**
     * Returns if the ProgramObject has a valid, compiled program. If false,
     * the compilation failed.
     */
    virtual bool valid() = 0;

    /**
     * Returns a KernelObject that represents the kernel whose name was given
     * as parameter.
     */
    virtual std::unique_ptr<KernelObject> createKernelObject(const std::string &name) = 0;

    /**
     * Returns the device of this ProgramObject.
     */
    inline Device *device() {
        return _device.get();
    }
};

#endif // !PROGRAMOBJECT_HPP
