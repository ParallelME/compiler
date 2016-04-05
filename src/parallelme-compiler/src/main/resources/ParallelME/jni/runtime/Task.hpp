#ifndef TASK_HPP
#define TASK_HPP

#include "Device.hpp"
#include "Kernel.hpp"
#include "Program.hpp"
#include <atomic>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>

/**
 * Represents a task from a program.
 */
class Task {

public:
    /// Hints for the scheduler.
    enum Hint {
        TargetCPU, TargetGPU
    };

    /// Hash that stores the kernels using their names as keys.
    typedef std::unordered_map<std::string, Kernel *> KernelHash;

    /**
     * Callback function called before the task is executed to configure the
     * task or after the task is executed to get the buffers back.
     */
    typedef std::function<void (Device &, KernelHash &)> KernelFunction;

    /**
     * Creates a task.
     * A task is composed of multiple kernels that are dependent of each other.
     * Different tasks can be executed concurrently.
     * @param program The program with the compiled code where the kernels are.
     * @param hint Hint of where the task should run faster.
     */
    Task(std::shared_ptr<Program> program, Hint hint = TargetGPU)
        : _program(program), _hint(hint) { }

    /**
     * This function prepares the Task to be executed by a worker. It is called
     * after the scheduler decides where the task will run on, so that buffers
     * can be created and set to the Kernels with only one copy for each device.
     */
    inline void setConfigFunction(KernelFunction configFunction) {
        std::unique_lock<std::mutex> lock(_mutex);
        _configFunction = configFunction;
    }

    /// Calls the configFunction currently set or does nothing if it wasn't set.
    inline void callConfigFunction(Device &device) {
        std::unique_lock<std::mutex> lock(_mutex);
        if(_configFunction)
            _configFunction(device, _kernelHash);
    }

    /**
     * This function gets the data back from the Task after it is executed by
     * a worker.
     */
    inline void setFinishFunction(KernelFunction finishFunction) {
        std::unique_lock<std::mutex> lock(_mutex);
        _finishFunction = finishFunction;
    }

    /// Calls the finishFunction currently set or does nothing if it wasn't set.
    inline void callFinishFunction(Device &device) {
        std::unique_lock<std::mutex> lock(_mutex);
        if(_finishFunction)
            _finishFunction(device, _kernelHash);
    }

    /**
     * Adds a kernel to the task.
     * The kernels are added in order to the execution vector, and are executed
     * in order.
     */
    inline Task *addKernel(const char *name) {
        std::unique_lock<std::mutex> lock(_mutex);
        _kernels.push_back(std::make_unique<Kernel>(name, *_program));
        _kernelHash.emplace(name, _kernels.back().get());
        return this;
    }

    /// Returns the vector of kernels.
    inline const std::vector< std::unique_ptr<Kernel> > &kernels() const {
        return _kernels;
    }

    /// Returns the task's hint (device where it should execute).
    inline Hint hint() const {
        return _hint;
    }

private:
    KernelFunction _configFunction;
    KernelFunction _finishFunction;
    std::shared_ptr<Program> _program;
    std::vector< std::unique_ptr<Kernel> > _kernels;
    std::mutex _mutex;
    KernelHash _kernelHash;
    Hint _hint;
};

#endif // !TASK_HPP
