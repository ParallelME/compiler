#ifndef TASK_HPP
#define TASK_HPP

#include <memory>
#include <mutex>
#include <unordered_map>
#include <vector>
#include <string>
#include "Device.hpp"
#include "Program.hpp"
#include "Buffer.hpp"
#include "Kernel.hpp"

/// Type of a pair of kernels identified by name.
typedef std::pair< std::string, std::shared_ptr<Kernel> > KernelPair;

/// Type of a hash of kernels identified by name.
typedef std::unordered_map< std::string, std::shared_ptr<Kernel> > KernelHash;

/// Type of a vector of kernels.
typedef std::vector< std::shared_ptr<Kernel> > KernelVector;

/**
 * Exception thrown if the task was created with an empty program vector.
 * The error message can be accessed through the what() function.
 */
class EmptyProgramError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if the user tried to access an invalid kernel.
 * The error message can be accessed through the what() function.
 */
class InvalidKernelError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Represents a single task to be executed by the target device. Can be
 * composed of multiple kernels.
 */
class Task {

public:
    Task(const Task &) = delete;
    Task &operator=(const Task &) = delete;

    ~Task() {  }

    /**
     * Represents how well the task runs at each device type.
     */
    struct Score {
        /// How fast the task runs at the CPU.
        float cpuScore;

        /// How fast the task runs at the GPU.
        float gpuScore;

        /// How fast the task runs at an Accelerator.
        float acceleratorScore;

        /// Initializes the scores with constructors or copy.
        Score(float cpu = 1.0f, float gpu = 1.0f, float acc = 1.0f)
            : cpuScore(cpu), gpuScore(gpu), acceleratorScore(acc) { }
    };

    /**
     * Callback function called before the task is executed to configure the
     * task or after the task is executed to get the buffers back.
     */
    typedef std::function<void (Device &, KernelHash &)> KernelFunction;

    /**
     * Creates a task.
     * A task is composed of multiple kernels that are dependent of each other.
     * Different tasks can be executed concurrently.
     * @param programs Vector of the programs available for the task execution.
     * This makes all the target devices that the programs supports available
     * for execution for the task. The order in which they are specified in
     * the vector also defines which programs have preference for execution.
     * For example, if you create a task with an OpenCL program specified first
     * and then a RenderScript program, OpenCL will be used for both CPU and
     * GPU devices if available, and RenderScript will be used as a CPU device
     * only if OpenCL CPU wasn't found. However, if you create a task with
     * a RenderScript program specified first and then a RenderScript program,
     * RenderScript will be used as the CPU device and OpenCL as the GPU device.
     * @param score How good the task is to execute at each device type. By
     * default the task is good to execute on all device types.
     */
    Task(Program::Vector &programs, Score score = Score());

    /**
     * Adds a kernel to the task.
     * The kernels are executed in the order they were added.
     */
    Task *addKernel(const std::string &name);

    /**
     * This function prepares the Task to be executed by a worker. It is called
     * after the scheduler decides where the task will run on, so that buffers
     * can be created and set to the Kernels with only one copy for each device.
     */
    inline void setConfigFunction(KernelFunction configFunction) {
        _configFunction = configFunction;
    }

    /**
     * This function gets the data back from the Task after it is executed by
     * a worker.
     */
    inline void setFinishFunction(KernelFunction finishFunction) {
        _finishFunction = finishFunction;
    }

    /**
     * Returns the score of the task/
     */
    inline const Score &score() {
        return _score;
    }

    /**
     * Returns the kernel with the given name or throws an exception.
     * This should only be used temporarily, and not stored.
     */
    inline Kernel &kernel(const std::string &name) {
        auto it = _kernelHash.find(name);
        if(it == _kernelHash.end())
            throw InvalidKernelError(std::string("Invalid kernel name: ") + name);

        return *it->second;
    }

    /// Calls the configFunction currently set or does nothing if it wasn't set.
    inline void callConfigFunction(Device &device) {
        if(_configFunction)
            _configFunction(device, _kernelHash);
    }

    /// Calls the finishFunction currently set or does nothing if it wasn't set.
    inline void callFinishFunction(Device &device) {
        if(_finishFunction)
            _finishFunction(device, _kernelHash);
    }

    /// Returns the map of program objects.
    inline const ProgramObjectMap &programObjects() {
         return _programObjects;
    }

    /// Returns the kernels in the order of execution.
    inline const KernelVector &kernels() {
        return _kernels;
    }

    /**
     * Returns if the Task already has a ProgramObject with the given device
     * type.
     */
    bool hasDeviceType(DeviceType type);

    /**
     * Returns if the Task has the given device ID.
     */
    inline bool hasDeviceID(DeviceID id) {
         return _programObjects.find(id) != _programObjects.end();
    }

private:
    Score _score;
    KernelFunction _configFunction;
    KernelFunction _finishFunction;
    ProgramObjectMap _programObjects; /// Device id -> program object.
    KernelHash _kernelHash; // Access kernel by name.
    KernelVector _kernels; // Order of execution.
};

#endif // !TASK_HPP
