/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_SCHEDULER_HEFT_HPP
#define PARALLELME_SCHEDULER_HEFT_HPP

#include <condition_variable>
#include <list>
#include <mutex>
#include "Scheduler.hpp"

namespace parallelme {

/**
 * An implementation of the Heterogeneous Earliest Finish Time scheduling
 * strategy for the ParallelME Runtime.
 *
 * @author Guilherme de Andrade, Renato Utsch
 */
class SchedulerHEFT : public Scheduler {
    std::list<std::unique_ptr<Task>> _cpuTaskList;
    std::list<std::unique_ptr<Task>> _gpuTaskList;
    std::list<std::unique_ptr<Task>> _globalTaskList;
    std::mutex _cpuMutex;
    std::mutex _gpuMutex;
    std::condition_variable _cvCpu, _cvGpu;

public:
    void push(std::unique_ptr<Task> task);
    std::unique_ptr<Task> pop(Device &device);
    void waitUntilIdle();
};

}

#endif // !PARALLELME_SCHEDULER_HEFT_PP
