#ifndef PROGRAM_HPP
#define PROGRAM_HPP

#include <memory>
#include <map>
#include <vector>
#include "Device.hpp"

class ProgramObject;

/**
 * Exception thrown if a program failed to compile on all platforms when being
 * constructed.
 * The compiler error messages can be accessed through the what() function.
 */
class ProgramCompilationError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/// Type of a ProgramObject pair of shared ptrs.
typedef std::pair< DeviceID, std::shared_ptr<ProgramObject> >
    ProgramObjectPair;

/// Type of a ProgramObject map of shared ptrs.
typedef std::map< DeviceID, std::shared_ptr<ProgramObject> >
    ProgramObjectMap;

/**
 * The Program class stores the ProgramObject's from each device the source
 * was able to compile to. When creating a task, you can supply source for
 * different platforms, and then these platforms will be available for the
 * task's execution.
 */
class Program {

protected:
    Program() { }

public:
    Program(const Program &) = delete;
    Program &operator=(const Program &) = delete;

    virtual ~Program() { }

    /// Vector of programs.
    typedef std::vector< std::shared_ptr<Program> > Vector;

    /**
     * Returns the list of available program objects for execution.
     */
    virtual ProgramObjectMap &programObjects() = 0;
};

#endif // !PROGRAM_HPP
