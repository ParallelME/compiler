#ifndef CL_CLPROGRAMOBJECT_HPP
#define CL_CLPROGRAMOBJECT_HPP

#include "../ProgramObject.hpp"
#include "CLDevice.hpp"
#include "CLKernelParams.hpp"

/**
 * This class implements the ProgramObject interface and allows you to compile
 * an OpenCL program.
 */
class CLProgramObject : public ProgramObject {
    cl_program _program;
    std::unique_ptr<CLKernelParams> _params;

    /**
     * Prints the build log to the error stream.
     */
    void printBuildLog(CLDevice *device);

public:
    /**
     * Constructs the ProgramObject.
     * Needs an OpenCL device, the source and compiler flags.
     */
    CLProgramObject(std::shared_ptr<Device> device, const char *source,
            const char *compilerFlags);

    inline bool valid() {
         return _program != nullptr;
    }

    std::unique_ptr<KernelObject> createKernelObject(const std::string &name);
};

#endif // !CL_CLPROGRAMOBJECT_HPP
