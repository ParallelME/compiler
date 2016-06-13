/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_SCHEDULER_FCFS_HPP
#define PARALLELME_SCHEDULER_FCFS_HPP

#include "Scheduler.hpp"
#include <condition_variable>
#include <list>
#include <mutex>

namespace parallelme {

/**
 * An implementation of the First Come First Served scheduling strategy for
 * the ParallelME Runtime.
 *
 * @author Guilherme de Andrade, Renato Utsch
 */
class SchedulerFCFS : public Scheduler {
    std::list<std::unique_ptr<Task>> _taskList;
    std::mutex _mutex;
    std::condition_variable _cv;

public:
    void push(std::unique_ptr<Task> task);
    std::unique_ptr<Task> pop(Device &device);
    void waitUntilIdle();
};

}

#endif // !PARALLELME_SCHEDULER_FCFS_HPP
