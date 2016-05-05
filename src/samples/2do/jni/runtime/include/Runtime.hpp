#ifndef RUNTIME_HPP
#define RUNTIME_HPP

#include <vector>
#include <memory>
#include <stdexcept>
#include <jni.h>
#include "Device.hpp"
#include "Task.hpp"
#include "Scheduler.hpp"
#include "sched/SchedulerFCFS.hpp"

class Loader;
class Worker;

/**
 * Exception thrown if the runtime failed to find at least one device from the
 * available execution engines.
 * The error message can be accessed through the what() function.
 */
class RuntimeNoDevicesError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * Exception thrown if the runtime failed to find at least one device from the
 * available execution engines.
 * The error message can be accessed through the what() function.
 */
class RuntimeExecutionError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * The Runtime class is responsible for managing all the contexts used by
 * ParallelME's runtime to execute a given kernel. It encapsulates the OpenCL
 * structures and the RenderScript context, for example, and is needed when
 * creating a CLProgram or a RSProgram.
 * When creating the runtime, a pointer to the JavaVM and the android context
 * can be passed to the constructor, along with changing the scheduler policy
 * to one different to the default (FCFS). To make the runtime run in an
 * Android device, at least the JavaVM pointer must be given, and to support
 * RenderScript a pointer to the android context (the "this" in the main
 * activity) is also required.
 */
class Runtime {
    std::vector< std::shared_ptr<Loader> > _loaders;    /// Vector of device loaders.
    DeviceVector _devices;                              /// Vector of devices.
    std::vector< std::shared_ptr<Worker> > _workers;    /// Runtime workers.
    std::shared_ptr<Scheduler> _scheduler;              /// Runtime scheduler.

    /// Initializes the devices.
    void loadDevices(JavaVM *jvm, jobject androidContext);

    /// Starts the workers.
    void startWorkers(JavaVM *jvm);

public:
    /**
     * Constructs the runtime.
     * @param sched the scheduler to be used by the runtime, defaulting to
     * First Come First Served.
     * @param jvm A pointer to the JavaVM. If this is specified, the worker
     * threads of the runtime will be linked to the JavaVM.
     * @param androidContext The Android Context class of the application. This
     * normally is the "this" member of the main activity. If this is not
     * specified, programming platforms that depend on this context (such as
     * RenderScript) will not be used as potential devices. This context also
     * depends on specifying a valid JavaVM on the first parameter.
     */
    Runtime(JavaVM *jvm = nullptr, jobject androidContext = nullptr,
            std::shared_ptr<Scheduler> &&sched = std::make_shared<SchedulerFCFS>())
            : _scheduler(sched) {
        loadDevices(jvm, androidContext);
        startWorkers(jvm);
    }

    Runtime(const Runtime &) = delete;
    Runtime &operator=(const Runtime &) = delete;

    ~Runtime();

    /**
     * Submits a task for execution. The runtime claims ownership to this class,
     * which will be deleted by it after the execution.
     */
    void submitTask(std::unique_ptr<Task> task);

    /**
     * Waits for all tasks to finish before returning.
     */
    void finish();

    /**
     * Returns the available devices from all platforms.
     */
    inline DeviceVector &devices() {
         return _devices;
    }
};

#endif // RUNTIME_HPP

