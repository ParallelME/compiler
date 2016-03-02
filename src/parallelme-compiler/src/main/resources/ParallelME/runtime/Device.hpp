#ifndef DEVICE_HPP
#define DEVICE_HPP

#include <clLoader.h>
#include <functional>
#include <mutex>
#include <stdexcept>

/**
 * Exception thrown if the device failed to construct itself.
 * The error messages can be accessed through the what() function.
 */
class DeviceConstructionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * RAII class that stores a device's data and only allow access to it through
 * mutexes.
 */
class Device {

public:
    /// Device type: GPU, CPU, etc.
    enum Type {
        CPU,
        GPU,
        Accelerator
    };

    /**
     * Constructs the device from the given OpenCL device. This claims ownership
     * to the device, and it shouldn't be used anymore without the helper
     * functions from this class.
     * The id is the position of the device in the runtime vector.
     * A context and queue are created from this device.
     */
    Device(cl_device_id device, unsigned id);

    ~Device();

    /**
     * This function calls fn with the device as parameter. It guarantees that
     * only one thread will be accessing the device at the same time.
     */
    inline void clDevice(std::function<void (cl_device_id)> fn) {
        std::unique_lock<std::mutex> lock(_mutex);
        fn(_clDevice);
    }

    /**
     * This function calls fn with the queue as parameter. It guarantees that
     * only one thread will be accessing the queue at the same time.
     */
    inline void clQueue(std::function<void (cl_command_queue)> fn) {
        std::unique_lock<std::mutex> lock(_mutex);
        fn(_clQueue);
    }

    /**
     * This function calls fn with the context as parameter. It guarantees that
     * only one thread will be accessing the context at the same time.
     */
    inline void clContext(std::function<void (cl_context)> fn) {
        std::unique_lock<std::mutex> lock(_mutex);
        fn(_clContext);
    }

    /// Returns the id of this device.
    inline unsigned id() const {
        return _id;
    }

    /// Returns the type of this device.
    inline Type type() const {
        return _type;
    }

private:
    cl_device_id _clDevice;
    cl_context _clContext;
    cl_command_queue _clQueue;
    std::mutex _mutex;
    unsigned _id;
    Type _type;
};

#endif // !DEVICE_HPP

