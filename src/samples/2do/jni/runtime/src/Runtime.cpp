#include <Runtime.hpp>
#include <Task.hpp>
#include "Worker.hpp"
#include "cl/CLLoader.hpp"
#include "util/platform.h"

#ifdef RUNTIME_ANDROID
#include "rs/RSLoader.hpp"
#endif // RUNTIME_ANDROID

Runtime::~Runtime() = default;

void Runtime::loadDevices(JavaVM *jvm, jobject androidContext) {
    // Get the OpenCL devices.
    std::shared_ptr<Loader> cl = std::make_shared<CLLoader>(jvm);
    _devices.insert(_devices.end(), cl->devices().begin(), cl->devices().end());

    // Loaders are alive until the runtime is destroyed for device cleanup.
    _loaders.push_back(std::move(cl));

#ifdef RUNTIME_ANDROID
    // Must have a valid jvm and androidContext if on Android.
    if(!jvm || !androidContext)
        throw std::runtime_error("Android applications must supply valid "
            "JavaVM and android context pointers to ParallelME's Runtime.");

    // RenderScript devices, only if has an android context.
    std::shared_ptr<Loader> rs = std::make_shared<RSLoader>(jvm, androidContext);
    _devices.insert(_devices.end(), rs->devices().begin(), rs->devices().end());

    _loaders.push_back(std::move(rs));
#endif // RUNTIME_ANDROID
}

void Runtime::startWorkers(JavaVM *jvm) {
    for(auto &device : _devices)
        _workers.push_back(std::make_shared<Worker>(device));
    for(auto &worker : _workers)
        worker->run(_scheduler, jvm);
}

void Runtime::submitTask(std::unique_ptr<Task> task) {
    _scheduler->push(std::move(task));

    for(auto &worker : _workers)
        worker->wakeUp();
}

void Runtime::finish() {
    while(_scheduler->hasWork()) { } // TODO: busy wait is not good.

    for(auto &worker : _workers)
        worker->finish();
}
