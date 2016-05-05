#ifndef CL_CLDEVICE_HPP
#define CL_CLDEVICE_HPP

#include <string>
#include <Device.hpp>
#include "dynloader/dynLoader.h"

class ProgramObject;

/**
 * Implementation of the Device class for OpenCL.
 */
class CLDevice : public Device {

public:
    /**
     * Initializes an OpenCL device.
     * @param device The OpenCL device ID.
     * @param type The type of the OpenCL device.
     * @param jvm The JavaVM instance.
     */
    CLDevice(cl_device_id device, DeviceType type, JavaVM *jvm);

    ~CLDevice();

    inline void finish() {
        int err = clFinish(_queue);
        if(err < 0)
            throw DeviceFinishError(std::to_string(err));
    }

    std::shared_ptr<Buffer> createBuffer(Buffer::ElementType type, int numX,
            int numY = 1, int numZ = 1);

    /// Returns the OpenCL device.
    inline cl_device_id clDevice() {
         return _device;
    }

    /// Returns the OpenCL context.
    inline cl_context clContext() {
        return _context;
    }

    /// Returns the OpenCL command queue.
    inline cl_command_queue clQueue() {
         return _queue;
    }

private:
    cl_device_id _device;
    cl_context _context;
    cl_command_queue _queue;
};

#endif // !CL_CLDEVICE_HPP
