/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <parallelme/SchedulerPAMS.hpp>
#include <parallelme/Device.hpp>
#include <parallelme/Program.hpp>
#include <parallelme/Task.hpp>
using namespace parallelme;

void SchedulerPAMS::push(std::unique_ptr<Task> task) {
    std::lock_guard<std::mutex> lock(_mutex);

    float speedUpCPU = task->score().gpuScore / task->score().cpuScore;
    float speedUpGPU = task->score().cpuScore / task->score().gpuScore;
    TaskInfo taskReferences;
    TaskInfoListIt cpuIt = _cpuTaskList.end();
    TaskInfoListIt gpuIt = _gpuTaskList.end();
    taskReferences.task = task.get();

    if(task->program().hasDeviceType(Device::GPU)) {
        if(_gpuTaskList.empty()) {
            gpuIt = _gpuTaskList.insert(gpuIt,
                TaskInfoPair(speedUpGPU, taskReferences));
        }
        else {
            for(auto it = _gpuTaskList.begin(); it != _gpuTaskList.end(); ++it) {
                if(speedUpGPU > it->first) {
                    gpuIt = _gpuTaskList.insert(it,
                        TaskInfoPair(speedUpGPU, taskReferences));
                    break;
                }
            }

            if(gpuIt == _gpuTaskList.end()) {
                gpuIt = _gpuTaskList.insert(gpuIt,
                    TaskInfoPair(speedUpGPU, taskReferences));
            }
        }
    }

    if(task->program().hasDeviceType(Device::CPU)) {
        if(_cpuTaskList.empty()) {
            cpuIt = _cpuTaskList.insert(cpuIt,
                TaskInfoPair(speedUpCPU, taskReferences));
        }
        else {
            for(auto it = _cpuTaskList.begin(); it != _cpuTaskList.end(); ++it) {
                if(speedUpCPU > it->first) {
                    cpuIt = _cpuTaskList.insert(it,
                        TaskInfoPair(speedUpCPU, taskReferences));
                    break;
                }
            }

            if(cpuIt == _cpuTaskList.end()) {
                 cpuIt = _cpuTaskList.insert(cpuIt,
                         TaskInfoPair(speedUpCPU, taskReferences));
            }
        }
    }

    if(task->program().hasDeviceType(Device::CPU)
            && task->program().hasDeviceType(Device::GPU)) {
        gpuIt->second.itCPU = cpuIt;
        gpuIt->second.itGPU = gpuIt;
        cpuIt->second.itCPU = cpuIt;
        cpuIt->second.itGPU = gpuIt;
    }
    else if(task->program().hasDeviceType(Device::CPU)) {
        cpuIt->second.itCPU = cpuIt;
    }
    else /* if(task->hasDeviceType(GPU)) */ {
        gpuIt->second.itGPU = gpuIt;
    }

    task.release();
}


std::unique_ptr<Task> SchedulerPAMS::pop(Device &device) {
    std::lock_guard<std::mutex> lock(_mutex);
    Task *retTask = nullptr;

    if(device.type() == Device::CPU) {
        auto it = _cpuTaskList.begin();

        if(!_cpuTaskList.empty()
                && it->second.task->program().hasDeviceID(device.id())) {
            retTask = it->second.task;

            if(retTask->program().hasDeviceType(Device::GPU))
                _gpuTaskList.erase(it->second.itGPU);
            _cpuTaskList.erase(it->second.itCPU);
        }
        else {
            _cv.notify_all();
            return nullptr;
        }
    }
    else if(device.type() == Device::GPU) {
        auto it = _gpuTaskList.begin();

        if(!_gpuTaskList.empty()
                && it->second.task->program().hasDeviceID(device.id())) {
            retTask = it->second.task;

            if(retTask->program().hasDeviceType(Device::CPU))
                _cpuTaskList.erase(it->second.itCPU);
            _gpuTaskList.erase(it->second.itGPU);
        }
        else {
            _cv.notify_all();
            return nullptr;
        }
    }

    return std::unique_ptr<Task>(retTask);
}

void SchedulerPAMS::waitUntilIdle() {
    std::unique_lock<std::mutex> lock(_mutex);
    for(;;) {
        if(_cpuTaskList.empty() && _gpuTaskList.empty())
            break;

        _cv.wait(lock);
    }
}

