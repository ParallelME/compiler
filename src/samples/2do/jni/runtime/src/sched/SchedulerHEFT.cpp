#include <sched/SchedulerHEFT.hpp>
#include <Task.hpp>

void SchedulerHEFT::push(std::unique_ptr<Task> task) {
    double gpuCountScore = task->score().gpuScore;
    double cpuCountScore = task->score().cpuScore;

    if(task->hasDeviceType(CPU)) {
        std::lock_guard<std::mutex> lock(_cpuMutex);
        for(auto &it : _cpuTaskList)
            cpuCountScore += it->score().cpuScore;
    }

    if(task->hasDeviceType(GPU)) {
        std::lock_guard<std::mutex> lock(_gpuMutex);
        for(auto &it : _gpuTaskList)
            gpuCountScore += it->score().gpuScore;
    }

    if(task->hasDeviceType(CPU) && task->hasDeviceType(GPU)) {
        if (cpuCountScore < gpuCountScore) {
            std::lock_guard<std::mutex> lock(_cpuMutex);
            _cpuTaskList.push_back(std::move(task));
        }
        else {
            std::lock_guard<std::mutex> lock(_gpuMutex);
            _gpuTaskList.push_back(std::move(task));
        }
    }
    else if(task->hasDeviceType(CPU)) {
        std::lock_guard<std::mutex> lock(_cpuMutex);
        _cpuTaskList.push_back(std::move(task));
    }
    else if(task->hasDeviceType(GPU)) {
        std::lock_guard<std::mutex> lock(_gpuMutex);
        _gpuTaskList.push_back(std::move(task));
    }
    else {
         throw std::runtime_error("Scheduler only supports CPU and GPU workers.");
    }
}


std::unique_ptr<Task> SchedulerHEFT::pop(Device &device){
    if(device.type() == CPU) {
        std::lock_guard<std::mutex> lock(_cpuMutex);
        if(!_cpuTaskList.empty() && _cpuTaskList.front()->hasDeviceID(device.id())) {
            std::unique_ptr <Task> retTask = std::move(_cpuTaskList.front());
            _cpuTaskList.pop_front();
            return retTask;
        }
        else {
            return nullptr;
        }
    }
    else if(device.type() == GPU) {
        std::lock_guard<std::mutex> lock(_gpuMutex);
        if(!_gpuTaskList.empty() && _gpuTaskList.front()->hasDeviceID(device.id())) {
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

