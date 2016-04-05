#ifndef PROGRAM_HPP
#define PROGRAM_HPP

#include <clLoader.h>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <vector>

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
 * This is a compiled program that can have many tasks.
 */
class Program {
    std::vector<cl_program> _clPrograms;
    std::mutex _mutex;

public:
    /**
     * Creates the program.
     * @param runtime The Runtime instance.
     * @param source The source code of the program.
     * @param compilerFlags Flags for the compiler.
     * If the compilation fails on all platforms, the constructor throws
     * ProgramCompilationError.
     */
    Program(std::shared_ptr<Runtime> runtime, const char *source,
            const char *compilerFlags);

    ~Program();

    /**
     * This function calls fn with the programs as parameters. It guarantees that
     * only one thread will be accessing the programs at the same time.
     */
    inline void clPrograms(std::function<void (std::vector<cl_program> &)> fn) {
        std::unique_lock<std::mutex> lock(_mutex);
        fn(_clPrograms);
    }
};

#endif // !PROGRAM_HPP
