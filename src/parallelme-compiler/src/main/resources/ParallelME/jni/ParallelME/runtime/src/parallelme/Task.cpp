/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <parallelme/Task.hpp>
#include <parallelme/Device.hpp>
#include <parallelme/Kernel.hpp>
#include <parallelme/Program.hpp>
#include "util/error.h"
using namespace parallelme;

Task::Task(std::shared_ptr<Program> program, Score score) : _score(score),
        _configFunction(nullptr), _program(program) {

}

Task::~Task() = default;

Task *Task::addKernel(const std::string &name) {
    _kernelNames.push_back(name);
    return this;
}

void Task::createKernels(std::shared_ptr<Device> &device) {
    for(auto &name : _kernelNames) {
        // I don't use std::make_shared here because Kernel's constructor is private.
        auto kernel = std::shared_ptr<Kernel>(new Kernel(name, device, *_program));

        _kernels.push_back(kernel);
        _kernelHash.insert(std::pair<std::string, Kernel *>(name, kernel.get()));
    }
}

void Task::run() {
    for(auto &kernel : _kernels)
        kernel->run();
}

