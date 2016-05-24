/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <parallelme/Runtime.hpp>
#include <parallelme/Task.hpp>
#include "Worker.hpp"
#include "dynloader/dynLoader.h"
using namespace parallelme;

Runtime::~Runtime() = default;

void Runtime::loadDevices() {
    if(!dynLoadOpenCL())
        throw RuntimeConstructionError("No OpenCL devices found.");

    int err;

    // Get the number of platforms.
    unsigned numPlatforms;
    err = clGetPlatformIDs(0, nullptr, &numPlatforms);
    if(err < 0)
        throw RuntimeConstructionError(std::to_string(err));

    // Get the platforms.
    auto platforms =
        std::unique_ptr<cl_platform_id []>{new cl_platform_id[numPlatforms]};
    err = clGetPlatformIDs(numPlatforms, platforms.get(), nullptr);
    if(err < 0)
        throw RuntimeConstructionError(std::to_string(err));

    // Initialize the devices for each platform.
    for(unsigned i = 0; i < numPlatforms; ++i) {
        unsigned numDevices;
        err = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, 0, nullptr,
                &numDevices);
        if(err < 0)
            throw RuntimeConstructionError(std::to_string(err));

        std::unique_ptr<cl_device_id []> devices{new cl_device_id[numDevices]};
        err = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, numDevices,
                devices.get(), nullptr);
        if(err < 0)
            throw RuntimeConstructionError(std::to_string(err));

        for(unsigned j = 0; j < numDevices; ++j)
            _devices.push_back(std::make_shared<Device>(devices[j]));
    }

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
    _scheduler->waitUntilIdle();

    for(auto &worker : _workers)
        worker->finish();
}
