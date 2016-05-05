#ifndef WORKER_HPP
#define WORKER_HPP

#include <condition_variable>
#include <memory>
#include <stdexcept>
#include <thread>
#include <jni.h>
#include <Device.hpp>
#include <Kernel.hpp>
#include <Task.hpp>
#include "KernelObject.hpp"

class Worker {
    std::shared_ptr<Device> _device;
    std::mutex _mutex;
    std::condition_variable _cv;
    bool _kill;
    bool _running;

    /// Executes a given task.
    void executeTask(std::unique_ptr<Task> task) {
        task->callConfigFunction(*_device);

        /// TODO: better encapsulate this call? Maybe a better design.
        for(const auto &kernel : task->kernels())
            kernel->objects()[_device->id()]->run(kernel->xDim(),
                    kernel->yDim(), kernel->zDim());

        _device->finish();

        task->callFinishFunction(*_device);
    }

public:
    /**
     * Constructs the worker from the given device.
     * This device must be kept alive while the worker exists.
     */
    Worker(std::shared_ptr<Device> device) : _device(device), _kill(false),
            _running(false) {

    }

    ~Worker() {
        /// Kills the worker, but only if it was without anything to do.
        /// This will block until the worker is killed.
        std::unique_lock<std::mutex> lock(_mutex);
        _kill = true;
        _cv.notify_one();
    }

    /**
     * Starts the worker. If the worker has already started this function does
     * nothing.
     * The worker asks for work to the scheduler until the sheduler returns a
     * nullptr pointer to a task. When this happens, it enters into sleep until
     * it is waken up by wakeUp() or killed by kill() by the runtime.
     */
    template<class Scheduler>
    void run(std::shared_ptr<Scheduler> scheduler, JavaVM *jvm) {
        if(_running)
            return;
        _running = true;

        std::thread t([=] () mutable {
            if(jvm) {
                JNIEnv *env;
                if(jvm->AttachCurrentThread(&env, nullptr))
                    throw std::runtime_error("failed to attach thread to JVM.");
            }

            std::unique_lock<std::mutex> lock(_mutex);

            for(;;) {
                auto task = scheduler->pop(*_device);

                if(task) {
                    executeTask(std::move(task));
                    continue;
                }
                if(_kill)
                    return;

                _cv.wait(lock);
            }

            if(jvm)
                jvm->DetachCurrentThread();
        });
        t.detach();
    }

    /// Waits for the worker to finish.
    inline void finish() {
        std::unique_lock<std::mutex> lock(_mutex);
    }

    /**
     * Wakes up the worker if it was sleeping because it didn't have anything
     * to do.
     */
    inline void wakeUp() {
        _cv.notify_one();
    }
};

#endif // !WORKER_HPP
