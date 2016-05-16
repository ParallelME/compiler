/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <parallelme/SchedulerHEFT.hpp>
#include <parallelme/Device.hpp>
#include <parallelme/Task.hpp>
#include <parallelme/Program.hpp>
using namespace parallelme;

void SchedulerHEFT::push(std::unique_ptr<Task> task) {
    double gpuCountScore = task->score().gpuScore;
    double cpuCountScore = task->score().cpuScore;

    if(task->program().hasDeviceType(Device::CPU)) {
        std::lock_guard<std::mutex> lock(_cpuMutex);
        for(auto &it : _cpuTaskList)
            cpuCountScore += it->score().cpuScore;
    }

    if(task->program().hasDeviceType(Device::GPU)) {
        std::lock_guard<std::mutex> lock(_gpuMutex);
        for(auto &it : _gpuTaskList)
            gpuCountScore += it->score().gpuScore;
    }

    if(task->program().hasDeviceType(Device::CPU)
            && task->program().hasDeviceType(Device::GPU)) {
        if (cpuCountScore < gpuCountScore) {
            std::lock_guard<std::mutex> lock(_cpuMutex);
            _cpuTaskList.push_back(std::move(task));
        }
        else {
            std::lock_guard<std::mutex> lock(_gpuMutex);
            _gpuTaskList.push_back(std::move(task));
        }
    }
    else if(task->program().hasDeviceType(Device::CPU)) {
        std::lock_guard<std::mutex> lock(_cpuMutex);
        _cpuTaskList.push_back(std::move(task));
    }
    else if(task->program().hasDeviceType(Device::GPU)) {
        std::lock_guard<std::mutex> lock(_gpuMutex);
        _gpuTaskList.push_back(std::move(task));
    }
    else {
         throw std::runtime_error("Scheduler only supports CPU and GPU workers.");
    }
}


std::unique_ptr<Task> SchedulerHEFT::pop(Device &device){
    if(device.type() == Device::CPU) {
        std::lock_guard<std::mutex> lock(_cpuMutex);
        if(!_cpuTaskList.empty()
                && _cpuTaskList.front()->program().hasDeviceID(device.id())) {
            std::unique_ptr <Task> retTask = std::move(_cpuTaskList.front());
            _cpuTaskList.pop_front();
            return retTask;
        }
        else {
            return nullptr;
        }
    }
    else if(device.type() == Device::GPU) {
        std::lock_guard<std::mutex> lock(_gpuMutex);
        if(!_gpuTaskList.empty()
                && _gpuTaskList.front()->program().hasDeviceID(device.id())) {
            std::unique_ptr <Task> retTask = std::move(_gpuTaskList.front());
            _gpuTaskList.pop_front();
            return retTask;
        }
        else {
            return nullptr;
        }
    }
    else {
        return nullptr;
    }
}

bool SchedulerHEFT::hasWork() {
    std::lock_guard<std::mutex> lockCPU(_cpuMutex);
    std::lock_guard<std::mutex> lockGPU(_gpuMutex);
    return !_cpuTaskList.empty() || !_gpuTaskList.empty();
}

