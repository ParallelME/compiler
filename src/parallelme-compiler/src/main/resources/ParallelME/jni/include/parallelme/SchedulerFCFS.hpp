/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_SCHEDULER_FCFS_HPP
#define PARALLELME_SCHEDULER_FCFS_HPP

#include <mutex>
#include <list>
#include "Scheduler.hpp"

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

public:
    /**
     * Pushes a task into the scheduler.
     * This function is thread-safe.
     */
    void push(std::unique_ptr<Task> task);

    /**
     * Pops a task from the scheduler.
     * This function is thread-safe.
     */
    std::unique_ptr<Task> pop(Device &device);

    /**
     * If the scheduler still has work to do.
     * This function is thread-safe.
     */
    inline bool hasWork() {
        std::unique_lock<std::mutex> lock(_mutex);
        return !_taskList.empty();
    }
};

}

#endif // !PARALLELME_SCHEDULER_FCFS_HPP
