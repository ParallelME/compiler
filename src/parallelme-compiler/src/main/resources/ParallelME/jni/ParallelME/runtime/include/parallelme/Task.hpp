/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_TASK_HPP
#define PARALLELME_TASK_HPP

#include <functional>
#include <memory>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <vector>

namespace parallelme {
class Buffer;
class Device;
class Kernel;
class Program;
class Worker;

/// Type of a hash of kernels identified by name.
typedef std::unordered_map<std::string, Kernel *> KernelHash;

/// Type of a pointer to a device.
typedef std::shared_ptr<Device> DevicePtr;

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
 *
 * @author Renato Utsch, Guilherme de Andrade
 */
class Task {

public:
    Task(const Task &) = delete;
    Task &operator=(const Task &) = delete;

    ~Task();

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
     * task.
     */
    typedef std::function<void (DevicePtr &, KernelHash &)> KernelFunction;

    /**
     * Creates a task.
     * A task is composed of multiple kernels that are dependent of each other.
     * Different tasks can be executed concurrently.
     * @param program Program with the kernels available for the task execution.
     * This makes all the target devices that the program supports available
     * for execution for the task.
     * @param score How good the task is to execute at each device type. By
     * default the task is good to execute on all device types.
     */
    Task(std::shared_ptr<Program> program, Score score = Score());

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
     * This function is called after the Task is executed by the worker. It can
     * be called to copy the content of the buffers back to host memory.
     */
    inline void setFinishFunction(KernelFunction finishFunction) {
        _finishFunction = finishFunction;
    }

    /**
     * Returns the score of the task.
     */
    inline const Score &score() {
        return _score;
    }

    /**
     * Returns the program of the task.
     */
    inline const Program &program() {
        return *_program;
    }

private:
    // Only the Worker can callFinishFunction().
    friend class Worker;

    /**
     * Creates the Kernels.
     */
    void createKernels(std::shared_ptr<Device> &device);

    /// Calls the configFunction currently set or does nothing if it wasn't set.
    inline void callConfigFunction(std::shared_ptr<Device> &device) {
        if(_configFunction)
            _configFunction(device, _kernelHash);
    }

    /// Calls the finishFunction currently set or does nothing if it wasn't set.
    inline void callFinishFunction(std::shared_ptr<Device> &device) {
        if(_finishFunction)
            _finishFunction(device, _kernelHash);
    }

    /**
     * Executes the kernels in the order they were created.
     */
    void run();

    Score _score;                       // Score of the task.
    KernelFunction _configFunction;     // Task's config function.
    KernelFunction _finishFunction;     // Task's finish function.
    std::shared_ptr<Program> _program;  // Program with the kernels.

    std::vector<std::string> _kernelNames; // Names of the kernels to be created.
    KernelHash _kernelHash; // Access kernel by name.
    std::vector<std::shared_ptr<Kernel>> _kernels; // Order of execution.
};

}

#endif // !PARALLELME_TASK_HPP
