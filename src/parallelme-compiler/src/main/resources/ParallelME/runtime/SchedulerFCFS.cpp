#include "SchedulerFCFS.hpp"

void SchedulerFCFS::push(std::unique_ptr<Task> task){
    std::unique_lock<std::mutex> lock(_mutex);
    _globalTaskList.push_back(std::move(task));
}


std::unique_ptr<Task> SchedulerFCFS::pop(Device &device){

    std::unique_lock<std::mutex> lock(_mutex);
    if(!_globalTaskList.empty()) {
        std::unique_ptr<Task> retTask = std::move(_globalTaskList.front());
        _globalTaskList.pop_front();
        return retTask;
    }
    else {
        return nullptr;
    }
}

bool SchedulerFCFS::hasWork() {
    std::unique_lock<std::mutex> lock(_mutex);
    return !_globalTaskList.empty();
}

