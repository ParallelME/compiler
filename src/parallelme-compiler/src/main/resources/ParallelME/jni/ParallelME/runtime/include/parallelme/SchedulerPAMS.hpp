/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_SCHEDULER_PAMS_HPP
#define PARALLELME_SCHEDULER_PAMS_HPP

#include "Scheduler.hpp"
#include <condition_variable>
#include <list>
#include <mutex>

namespace parallelme {

/**
 * An implementation of the Performance Aware Multi-queue Scheduling
 * strategy for the ParallelME Runtime.
 *
 * @author Guilherme de Andrade, Renato Utsch
 */
class SchedulerPAMS : public Scheduler {
    struct TaskInfo;
    typedef std::pair<float, TaskInfo> TaskInfoPair;
    typedef std::list<TaskInfoPair> TaskInfoList;
    typedef TaskInfoList::iterator TaskInfoListIt;

    struct TaskInfo {
        TaskInfoListIt itGPU;
        TaskInfoListIt itCPU;
        Task *task;
    };
    TaskInfoList _cpuTaskList;
    TaskInfoList _gpuTaskList;
    std::mutex _mutex;
    std::condition_variable _cv;

public:
    void push(std::unique_ptr<Task> task);
    std::unique_ptr<Task> pop(Device &device);
    void waitUntilIdle();
};

}

#endif // !PARALLELME_SCHEDULER_FCFS_HPP
