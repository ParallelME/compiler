/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <parallelme/Kernel.hpp>
#include <parallelme/Buffer.hpp>
#include <parallelme/Device.hpp>
#include <parallelme/Program.hpp>
#include <string>
#include "dynloader/dynLoader.h"
using namespace parallelme;

Kernel::Kernel(const std::string &name, std::shared_ptr<Device> device,
        Program &program) : _device(device) {
    int err;

    _clKernel = clCreateKernel(program.clProgram(device->id()), name.c_str(), &err);
    if(err < 0)
        throw KernelConstructionError(std::to_string(err));
}

Kernel::~Kernel() {
    if(_clKernel) {
        clReleaseKernel(_clKernel);
        _clKernel = nullptr;
    }
}

void Kernel::run() {
    size_t offset[] = { 0, 0, 0 };
    size_t workSize[] = { _xDim, _yDim, _zDim };
    int err = clEnqueueNDRangeKernel(_device->clQueue(), _clKernel, 3, offset,
            workSize, nullptr, 0, nullptr, nullptr);
    if(err < 0)
        throw KernelExecutionError(std::to_string(err));
}

Kernel *Kernel::setArg(unsigned id, std::shared_ptr<Buffer> buffer) {
    int err;
    auto mem = buffer->clMem(_device);

    err = clSetKernelArg(_clKernel, id, sizeof(mem), &mem);
    if(err < 0)
        throw KernelArgError(std::string("Buffer error: ") + std::to_string(err));

    return this;
}

Kernel *Kernel::setPrimitiveArg(unsigned id, size_t size, void *host) {
    int err;

    err = clSetKernelArg(_clKernel, id, size, host);
    if(err < 0)
        throw KernelArgError(std::string("Primitive error: ") + std::to_string(err));

    return this;
}

