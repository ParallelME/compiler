#ifndef SCHEDULER_HEFT_HPP
#define SCHEDULER_HEFT_HPP

#include <list>
#include <mutex>
#include "../Scheduler.hpp"

/**
 * An implementation of the Heterogeneous Earliest Finish Time scheduling
 * strategy for the ParallelME Runtime.
 */
class SchedulerHEFT : public Scheduler {
    std::list< std::unique_ptr<Task> > _cpuTaskList;
    std::list< std::unique_ptr<Task> > _gpuTaskList;
    std::mutex _cpuMutex;
    std::mutex _gpuMutex;
    std::list< std::unique_ptr<Task> > _globalTaskList;

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

#endif // !SCHEDULER_HEFT_PP
