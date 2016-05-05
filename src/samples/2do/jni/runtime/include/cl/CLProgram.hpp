#ifndef CL_CLPROGRAM_HPP
#define CL_CLPROGRAM_HPP

#include "../Program.hpp"
#include <unordered_map>
#include <string>

class Runtime;

/**
 * Implementation of the Program class for OpenCL. Use it to compile your
 * OpenCL program to be used by the Runtime.
 */
class CLProgram : public Program {
    ProgramObjectMap _programObjects;

    /// Maps the parameter name to its position on the kernel's prototype.
    typedef std::unordered_map<std::string, unsigned> ParameterNameHash;

    /// Maps the kernel name to its parameters.
    typedef std::unordered_map<std::string, ParameterNameHash> KernelParamsHash;

    /// Kernel parameters.
    KernelParamsHash _kernelParams;

public:
     /**
      * Creates the program.
      * @param runtime The Runtime instance.
      * @param source The source code of the program.
      * @param compilerFlags Flags for the compiler.
      * If the compilation fails on all platforms, the constructor throws
      * ProgramCompilationError.
      */
    CLProgram(std::shared_ptr<Runtime> runtime, const char *source,
            const char *compilerFlags = nullptr);

    ~CLProgram();

    inline ProgramObjectMap &programObjects() {
         return _programObjects;
    }
};

#endif // !CL_CLPROGRAM_HPP
