#ifndef DEVICE_HPP
#define DEVICE_HPP

#include <memory>
#include <stdexcept>
#include <vector>
#include "Buffer.hpp"

class Device;

/// Type of a device id.
typedef unsigned DeviceID;

/// Vector of devices.
typedef std::vector< std::shared_ptr<Device> > DeviceVector;

/**
 * Platforms a device can be from.
 */
enum DevicePlatform {
     OpenCL,
     RenderScript
};

/**
 * Device type.
 */
enum DeviceType {
    CPU,
    GPU,
    Accelerator
};

/**
 * Exception thrown if the device failed to construct and initialize.
 * The error message can be accessed through the what() function.
 */
class DeviceConstructionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if the device failed to finish execution.
 * The error message can be accessed through the what() function.
 */
class DeviceFinishError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};


/**
 * This is an abstract class that defines the interface that should be
 * implemented by a device of a given programming platform to work with
 * the Runtime system. Each different programming platform creates one
 * Device class for each execution device present in their system, and the
 * Runtime class registers all these devices to be used on execution.
 */
class Device {

public:
    Device(const Device &) = delete;
    Device &operator=(const Device &) = delete;

    virtual ~Device() {  }

    /**
     * Returns the device's platform.
     */
    inline DevicePlatform platform() const {
         return _platform;
    }

    /**
     * Returns the device type.
     */
    inline DeviceType type() const {
         return _type;
    }

    /**
     * Returns the device's ID.
     */
    inline DeviceID id() const {
        return _id;
    }

    /**
     * Returns the JavaVM.
     */
    inline JavaVM *jvm() {
        return _jvm;
    }

    /// Returns the JNIEnv of the current thread.
    inline JNIEnv *jenv() {
        JNIEnv *env;
        int status = _jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
        if(status != JNI_OK)
            throw std::runtime_error("failed to get the JNIEnv of the thread.");

        return env;
    }

    /**
     * Blocks until all executions queued for this device finishes.
     */
    virtual void finish() = 0;

    /**
     * Creates a buffer on the device's memory and returns it.
     * @param type The type of the elements of the buffer.
     * @param numX The number of elements of the X dimension of the buffer.
     * @param numY The number of elements of the Y dimension of the buffer.
     * @param numZ The number of elements of the Z dimension of the buffer.
     * @see setConfigFunction
     */
    virtual std::shared_ptr<Buffer> createBuffer(Buffer::ElementType type,
            int numX, int numY = 1, int numZ = 1) = 0;

private:
    /// Generates an unique ID that identifies a new device.
    static inline DeviceID genID() {
        static DeviceID idSeed = 0; // Just increment this and new ID every call.
        return idSeed++;
    }

protected:
    /**
     * Initializes the device type.
     */
    Device(DevicePlatform platform, DeviceType type, JavaVM *jvm)
            : _platform(platform), _type(type), _id(genID()), _jvm(jvm) {  }

private:
    DevicePlatform _platform; /// The platform of this device.
    DeviceType _type;         /// The type of this device.
    DeviceID _id;             /// The unique device id.
    JavaVM *_jvm;             /// JavaVM instance.
};

#endif // !DEVICE_HPP
