#include "Worker.hpp"
#include "Runtime.hpp"
#include <clLoader.h>


Worker::Worker(std::shared_ptr<Device> device)
        : _device(device), _kill{false}, _running{false} {

}

Worker::~Worker() {
    /// Kills the worker, but only if it was without anything to do.
    /// This will block until the worker is killed.
    std::unique_lock<std::mutex> lock(_mutex);
    _kill = true;
    _cv.notify_one();

}

std::mutex gKernelMutex;

void Worker::executeTask(std::unique_ptr<Task> task) {
    task->callConfigFunction(*_device);

    for(const auto &kernel : task->kernels()) {
        _device->clQueue([&] (cl_command_queue queue) {
            int err = 0;
            size_t offset[] = { kernel->offset() };
            size_t workSize[] = { kernel->workSize() };

            kernel->clKernel(_device->id(), [&] (cl_kernel kernel) {
                std::unique_lock<std::mutex> lock(gKernelMutex);
                err = clEnqueueNDRangeKernel(queue, kernel, 1, offset, workSize,
                        NULL, 0, NULL, NULL);
                if(err < 0)
                    throw WorkerExecutionError(std::to_string(err));
            });

            err = clFinish(queue);
            if(err < 0)
                throw WorkerExecutionError(std::to_string(err));
        });
    }

    task->callFinishFunction(*_device);
}

