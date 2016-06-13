/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <parallelme/Device.hpp>
#include <parallelme/Program.hpp>
#include <parallelme/Runtime.hpp>
#include <string>
#include "dynloader/dynLoader.h"
#include "util/error.h"
using namespace parallelme;


Program::Program(std::shared_ptr<Runtime> runtime, const char *source,
        const char *compilerFlags) {
    int err;

    for(auto &device : runtime->devices()) {
        auto program = clCreateProgramWithSource(device->clContext(), 1, &source,
                nullptr, &err);
        if(err < 0)
            throw ProgramCompilationError(std::to_string(err));

        err = clBuildProgram(program, 0, nullptr, compilerFlags, nullptr, nullptr);
        if(err < 0) {
            printBuildLog(program, *device);
            clReleaseProgram(program);
        }
        else {
            _programs.insert(std::pair<unsigned, _cl_program *>(device->id(),
                        program));
            _deviceTypes.insert(device->type());
        }
    }
}

Program::~Program() {
    for(auto &it : _programs)
        clReleaseProgram(it.second);
}

void Program::printBuildLog(_cl_program *program, Device &device) {
    size_t logSize;
    int err;

    err = clGetProgramBuildInfo(program, device.clDevice(),
            CL_PROGRAM_BUILD_LOG, 0, nullptr, &logSize);
    if(err < 0)
        throw ProgramCompilationError(std::to_string(err));

    std::unique_ptr<char []> programLog{new char[logSize]};

    err = clGetProgramBuildInfo(program, device.clDevice(),
            CL_PROGRAM_BUILD_LOG, logSize, programLog.get(), nullptr);
    if(err < 0)
        throw ProgramCompilationError(std::to_string(err));

    printError("OpenCL Kernel compilation failed:\n%s\n", programLog.get());
}

