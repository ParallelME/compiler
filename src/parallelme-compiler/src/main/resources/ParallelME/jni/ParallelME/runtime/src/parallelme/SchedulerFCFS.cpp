/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <parallelme/SchedulerFCFS.hpp>
#include <parallelme/Device.hpp>
#include <parallelme/Program.hpp>
#include <parallelme/Task.hpp>
using namespace parallelme;

void SchedulerFCFS::push(std::unique_ptr<Task> task){
    std::unique_lock<std::mutex> lock(_mutex);
    _taskList.push_back(std::move(task));
}

std::unique_ptr<Task> SchedulerFCFS::pop(Device &device){
    std::unique_lock<std::mutex> lock(_mutex);

    if(!_taskList.empty()
            && _taskList.front()->program().hasDeviceID(device.id())) {
        std::unique_ptr<Task> retTask = std::move(_taskList.front());
        _taskList.pop_front();
        return retTask;
    }
    else {
        _cv.notify_all();
        return nullptr;
    }
}

void SchedulerFCFS::waitUntilIdle() {
    std::unique_lock<std::mutex> lock(_mutex);
    for(;;) {
        if(_taskList.empty())
            break;

        _cv.wait(lock);
    }
}
