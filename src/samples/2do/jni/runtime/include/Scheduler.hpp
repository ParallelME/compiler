#ifndef SCHEDULER_HPP
#define SCHEDULER_HPP

#include <vector>
#include "Device.hpp"
#include "Task.hpp"

/**
 * The Scheduler class specifies an interface that is used by all the different
 * scheduling policies implemented by the Runtime. New scheduling policies can
 * also be created by deriving this class and using the derived class when
 * constructing a Runtime class instance.
 */
class Scheduler {

public:
    Scheduler() = default;

    Scheduler(const Scheduler &) = delete;
    Scheduler &operator=(const Scheduler &) = delete;

    virtual ~Scheduler() = default;

    /**
     * Pushes a task into the scheduler.
     * This function is thread-safe.
     */
    virtual void push(std::unique_ptr<Task> task) = 0;

    /**
     * Pops a task from the scheduler.
     * This function is thread-safe.
     */
    virtual std::unique_ptr<Task> pop(Device &device) = 0;

    /**
     * If the scheduler still has work to do.
     * This function is thread-safe.
     */
    virtual bool hasWork() = 0;
};

#endif // !SCHEDULER_HPP
