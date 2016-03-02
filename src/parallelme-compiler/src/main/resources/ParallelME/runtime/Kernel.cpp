#include "Kernel.hpp"
#include "Program.hpp"
#include <string>

Kernel::Kernel(const char *name, Program &program) : _offset(0), _workSize(1) {
    int err;

    program.clPrograms([&] (std::vector<cl_program> &clPrograms) {
        for(auto &clProgram : clPrograms) {
            cl_kernel kernel = clCreateKernel(clProgram, name, &err);
            if(err < 0)
                throw KernelConstructionError(std::to_string(err));

            _clKernels.push_back(kernel);
        }
    });
}

Kernel::~Kernel() {
    for(auto &kernel : _clKernels)
        clReleaseKernel(kernel);
}
