#include "Program.hpp"
#include "Runtime.hpp"
#include <sstream>
#include "../error.h"

Program::Program(std::shared_ptr<Runtime> runtime, const char *source,
            const char *compilerFlags) {
    int err;

    for(auto &device : runtime->devices()) {
        cl_program program = nullptr;

        device->clContext([&] (cl_context context) {
            program = clCreateProgramWithSource(context, 1, &source, NULL, &err);
            if(err < 0)
                throw ProgramCompilationError(std::to_string(err));
        });

        err = clBuildProgram(program, 0, NULL, compilerFlags, NULL, NULL);
        if(err < 0) {
            device->clDevice([=] (cl_device_id device) mutable {
                std::stringstream ss;
                size_t logSize;

                err = clGetProgramBuildInfo(program, device,
                    CL_PROGRAM_BUILD_LOG, 0, nullptr, &logSize);
                if(err < 0)
                    throw ProgramCompilationError(std::to_string(err));

                std::unique_ptr<char []> programLog{new char[logSize + 1]};
                programLog[logSize] = '\0';

                err = clGetProgramBuildInfo(program, device,
                    CL_PROGRAM_BUILD_LOG, logSize + 1, programLog.get(), nullptr);
                if(err < 0)
                    throw ProgramCompilationError(std::to_string(err));

                ss << "OpenCL Kernel compilation failed:\n"
                    << programLog.get() << "\n";

                throw ProgramCompilationError(ss.str());
            });
        }

        _clPrograms.push_back(std::move(program));
    }
}

Program::~Program() {
    for(auto &program : _clPrograms)
        clReleaseProgram(program);
}

