#ifndef SCHEDULER_FCFS_HPP
#define SCHEDULER_FCFS_HPP

#include "Scheduler.hpp"

class SchedulerFCFS : public Scheduler {
    std::list< std::unique_ptr<Task> > _globalTaskList;
    std::mutex _mutex;

public:
    SchedulerFCFS(std::vector< std::shared_ptr<Device> > &devices)
        : Scheduler(devices) {
        // For FCFS nothing to set up.
    }

    ~SchedulerFCFS() {
        // For FCFS nothing to destroy.
    }

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
