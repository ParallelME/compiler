/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_PROGRAM_HPP
#define PARALLELME_PROGRAM_HPP

#include <map>
#include <set>
#include <stdexcept>
#include "Device.hpp"

struct _cl_program;

namespace parallelme {
class Runtime;

/**
 * Exception thrown if a program failed to compile on all platforms when being
 * constructed.
 * The compiler error messages can be accessed through the what() function.
 */
class ProgramCompilationError : public std::runtime_error {
    using std::runtime_error::runtime_error;
};

/**
 * The Program class stores the program objects from each device the source
 * was able to compile to.
 *
 * @author Renato Utsch
 */
class Program {
    std::map<unsigned, _cl_program *> _programs;    /// Maps device id to program.
    std::set<Device::Type> _deviceTypes;            /// Set of the device types.

    /// Prints the build log to the error stream.
    void printBuildLog(_cl_program *program, Device &device);

public:
    /**
     * Creates the program. A program is the compiled source that can be executed
     * on a device.
     * @param runtime The runtime instance.
     * @param source The source code of the program.
     * @param compilerFlags The flags for the OpenCL compiler.
     * If the compilation fails on all platforms, the constructor throws a
     * ProgramCompilationError.
     */
    Program(std::shared_ptr<Runtime> runtime, const char *source,
            const char *compilerFlags = nullptr);

    Program(const Program &) = delete;
    Program &operator=(const Program &) = delete;

    ~Program();

    /**
     * Returns the cl_program for the given device.
     */
    inline _cl_program *clProgram(unsigned deviceID) {
        return _programs[deviceID];
    }

    /**
     * Returns if the program has a device ID.
     */
    inline bool hasDeviceID(unsigned id) const {
        return _programs.find(id) != _programs.end();
    }

    /**
     * Returns if the program has a device type.
     */
    bool hasDeviceType(Device::Type type) const {
         return _deviceTypes.find(type) != _deviceTypes.end();
    }
};

}

#endif // !PARALLELME_PROGRAM_HPP
