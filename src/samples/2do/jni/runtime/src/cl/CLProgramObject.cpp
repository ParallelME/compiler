#include "CLProgramObject.hpp"
#include "CLKernelObject.hpp"
#include "../util/error.h"
#include <Program.hpp>
#include <string>
#include <cstdlib>

CLProgramObject::CLProgramObject(std::shared_ptr<Device> device,
        const char *source, const char *compilerFlags) : ProgramObject(device),
        _program(nullptr), _params(nullptr) {
    int err;

    auto cldev = (CLDevice *) device.get();

    _program = clCreateProgramWithSource(cldev->clContext(), 1, &source,
            nullptr, &err);
    if(err < 0)
        throw ProgramCompilationError(std::to_string(err));

    err = clBuildProgram(_program, 0, nullptr, compilerFlags, nullptr, nullptr);
    if(err < 0) {
        printBuildLog(cldev);
        _program = nullptr;
    }
    else { // Save the kernel params, as the build suceeded.
        _params = std::make_unique<CLKernelParams>(source);
    }
}

void CLProgramObject::printBuildLog(CLDevice *device) {
    size_t logSize;
    int err;

    err = clGetProgramBuildInfo(_program, device->clDevice(),
            CL_PROGRAM_BUILD_LOG, 0, nullptr, &logSize);
    if(err < 0)
        throw ProgramCompilationError(std::to_string(err));

    std::unique_ptr<char []> programLog{new char[logSize]};

    err = clGetProgramBuildInfo(_program, device->clDevice(),
            CL_PROGRAM_BUILD_LOG, logSize, programLog.get(), nullptr);
    if(err < 0)
        throw ProgramCompilationError(std::to_string(err));

    printError("OpenCL Kernel compilation failed:\n%s\n", programLog.get());
}

std::unique_ptr<KernelObject> CLProgramObject::createKernelObject(
        const std::string &name) {
    return std::make_unique<CLKernelObject>(name, _device, _program,
                _params->kernelParams(name));
}

