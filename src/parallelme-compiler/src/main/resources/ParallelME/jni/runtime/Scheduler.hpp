#ifndef SCHEDULER_HPP
#define SCHEDULER_HPP

#include <vector>
#include "Device.hpp"
#include "Worker.hpp"
#include "Task.hpp"
#include <mutex>
#include <list>

class Scheduler {

public:
    Scheduler(std::vector< std::shared_ptr<Device> > &devices) { }
    virtual ~Scheduler() { }

    /**
     * Pushes a task into the scheduler.
     * This function is thread-safe.
     */
    virtual void push(std::unique_ptr<Task> task);

    /**
     * Pops a task from the scheduler.
     * This function is thread-safe.
     */
    virtual std::unique_ptr<Task> pop(Device &device);

    /**
     * If the scheduler still has work to do.
     * This function is thread-safe.
     */
    virtual bool hasWork();
};

#endif // !SCHEDULER_HPP
