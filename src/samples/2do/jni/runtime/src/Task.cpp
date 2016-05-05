#include <Task.hpp>
#include <Program.hpp>
#include <Kernel.hpp>
#include "ProgramObject.hpp"
#include "Device.hpp"
#include "util/error.h"
Task::Task(Program::Vector &programs, Score score) : _score(score) {
    if(programs.empty())
        throw EmptyProgramError("No programs specified.");

    // For each program, add the devices that don't yet have an execution
    // platform for this task.
    for(auto &program : programs)
        for(auto &obj : program->programObjects())
            if(!hasDeviceType(obj.second->device()->type()))
                _programObjects.insert(ProgramObjectPair(obj.first, obj.second));
}

bool Task::hasDeviceType(DeviceType type) {
    for(auto &obj : _programObjects)
        if(obj.second->device()->type() == type)
            return true;

    return false;
}

Task *Task::addKernel(const std::string &name) {
    auto kernel = std::make_shared<Kernel>(name, _programObjects);
    _kernels.push_back(kernel);
    _kernelHash.insert(KernelPair(name, kernel));

    return this;
}
