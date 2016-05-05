#ifndef SCHEDULER_FCFS_HPP
#define SCHEDULER_FCFS_HPP

#include <mutex>
#include <list>
#include "../Scheduler.hpp"

/**
 * An implementation of the First Come First Served scheduling strategy for
 * the ParallelME Runtime.
 */
class SchedulerFCFS : public Scheduler {
    std::list< std::unique_ptr<Task> > _globalTaskList;
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
    bool hasWork();
};

#endif // !SCHEDULER_FCFS_HPP
