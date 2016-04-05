#ifndef RUNTIME_HPP
#define RUNTIME_HPP

#include "Task.hpp"
#include "Scheduler.hpp"
#include "SchedulerFCFS.hpp"
#include <clLoader.h>
#include <memory>
#include <mutex>
#include <vector>
#include <list>
#include <set>
#include <new>
#include <condition_variable>
#include <stdexcept>
#include <jni.h>
#include "../error.h"

/**
 * Exception thrown if the runtime failed to initialize the underlying runtime
 * library (ex. OpenCL).
 * The error messages can be accessed through the what() function.
 */
class RuntimeConstructionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};


/**
 * This class represents the Runtime exposed to the user.
 */
class Runtime {
    std::vector< std::shared_ptr<Device> > _devices;
    std::vector< std::unique_ptr<Worker> > _workers;
    std::shared_ptr<Scheduler> _scheduler;

public:
    /**
     * Constructs the runtime. If a JavaVM is specified, the worker threads of
     * the runtime will be linked to the JavaVM.
     * The Sched class is the scheduler to be used by the runtime, defaulting to
     * First Come First Served. It must be a derived class from Scheduler.
     */
    template<class Sched = SchedulerFCFS>
    Runtime(JavaVM *jvm = nullptr) {
        int err;

        if(!loadOpenCL())
            throw RuntimeConstructionError("Failed to load the OpenCL library.");

        // Get the number of platforms.
        unsigned numPlatforms;
        err = clGetPlatformIDs(0, NULL, &numPlatforms);

        // Get the platforms.
        auto platforms =
            std::unique_ptr<cl_platform_id []>{new cl_platform_id[numPlatforms]};
        err = clGetPlatformIDs(numPlatforms, platforms.get(), NULL);
        if(err < 0)
            throw RuntimeConstructionError(std::to_string(err));

        // Initialize the devices for each platform.
        unsigned id = 0;
        for(unsigned i = 0; i < numPlatforms; ++i) {
            unsigned numDevices;
            err = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, 0, NULL,
                    &numDevices);
            if(err < 0)
                throw RuntimeConstructionError(std::to_string(err));

            std::unique_ptr<cl_device_id []> devices{new cl_device_id[numDevices]};
            err = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, numDevices,
                    devices.get(), nullptr);
            if(err < 0)
                throw RuntimeConstructionError(std::to_string(err));

            for(unsigned j = 0; j < numDevices; ++j)
                _devices.push_back(std::make_shared<Device>(devices[j], id++));
        }

        _scheduler = std::make_shared<Sched>(_devices);

        for(unsigned i = 0; i < _devices.size(); ++i)
            _workers.push_back(std::make_unique<Worker>(_devices[i]));
        for(auto &worker : _workers)
            worker->run(_scheduler, jvm);
    }

    ~Runtime() {
        closeOpenCL();
    }

    /**
     * Submits a dynamically allocated task for execution. The runtime claims
     * ownership to this class, which will be deleted by it.
     */
    inline void submitTask(std::unique_ptr<Task> task) {
        _scheduler->push(std::move(task));

        for(auto &worker : _workers)
            worker->wakeUp();
    }

    /**
     * Waits for all tasks to be finish.
     */
    void finish() {
        while(_scheduler->hasWork()) { } // TODO Busy wait is not good.

        for(auto &worker : _workers)
            worker->finish();
    }

    /// Returns the devices
    inline std::vector< std::shared_ptr<Device> > &devices() {
        return _devices;
    }
};

#endif // RUNTIME_HPP
