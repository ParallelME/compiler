/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_DEVICE_HPP
#define PARALLELME_DEVICE_HPP

#include <memory>
#include <stdexcept>
#include <jni.h>

struct _cl_device_id;
struct _cl_context;
struct _cl_command_queue;

namespace parallelme {

class Worker;

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
 *
 * @author Renato Utsch
 */
class Device {

public:
    /**
     * Device type.
     */
    enum Type {
        CPU,
        GPU,
        Accelerator
    };

    /**
     * Initializes the device.
     */
    Device(_cl_device_id *id);

    Device(const Device &) = delete;
    Device &operator=(const Device &) = delete;

    ~Device();

    /**
     * Returns the device type.
     */
    inline Type type() const {
         return _type;
    }

    /**
     * Returns the device ID.
     */
    inline unsigned id() {
        return _id;
    }

    /**
     * Returns the JNIEnv of the device's thread.
     */
    inline JNIEnv *JNIEnv() {
         return _env;
    }

    /**
     * Blocks until all executions already queued for this device finish.
     */
    void finish();

    /// Returns the cl_device_id.
    inline _cl_device_id *clDevice() {
         return _clDevice;
    }

    /// Returns the cl_context.
    inline _cl_context *clContext() {
         return _clContext;
    }

    /// Returns cl_command_queue.
    inline _cl_command_queue *clQueue() {
         return _clQueue;
    }

private:
    friend class Worker;

    /**
     * Sets the JNIEnv of the device. Only the Worker class should call this.
     */
    inline void setJNIEnv(_JNIEnv *env) {
        _env = env;
    }

    /// Generates a new device ID, starting from 0.
    static inline unsigned genID() {
        static unsigned newID = 0;
        return newID++;
    }

    /// Returns the type of the given device id.
    static Type findType(_cl_device_id *clDevice);

    _cl_device_id *_clDevice;       /// OpenCL Device ID.
    _cl_context *_clContext;        /// OpenCL context.
    _cl_command_queue *_clQueue;    /// OpenCL command queue.
    Type _type;                     /// The type of this device.
    unsigned _id;                   /// Device ID.
    _JNIEnv *_env;                   /// JNIEnv of the device's thread.
};

}

#endif // !PARALLELME_DEVICE_HPP
