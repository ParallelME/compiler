#include "CLKernelObject.hpp"
#include "CLBuffer.hpp"
#include "CLDevice.hpp"
#include <Kernel.hpp>
#include <string>

CLKernelObject::CLKernelObject(const std::string &name, std::shared_ptr<Device> device,
        cl_program clProgram, const KernelParamHash &params)
            : KernelObject(device), _kernel(nullptr), _params(params) {
    int err;

    _kernel = clCreateKernel(clProgram, name.c_str(), &err);
    if(err < 0)
        throw KernelConstructionError(std::to_string(err));
}

void CLKernelObject::run(int xDim, int yDim, int zDim) {
    size_t execOffset[] = { 0, 0, 0 };
    size_t execWorkSize[] = { (size_t) xDim, (size_t) yDim, (size_t) zDim };

    auto clDev = (CLDevice *) _device.get();
    int err = clEnqueueNDRangeKernel(clDev->clQueue(), _kernel, 3, execOffset,
            execWorkSize, nullptr, 0, nullptr, nullptr);
    if(err < 0)
        throw KernelExecutionError(std::to_string(err));
}

void CLKernelObject::setInputBuffer(std::shared_ptr<Buffer> buffer) {
    auto clBuffer = (CLBuffer *) buffer.get();
    auto clMem = clBuffer->clMem();
    const unsigned InputBufferIndex = 0;

    int err = clSetKernelArg(_kernel, InputBufferIndex, sizeof(clMem), &clMem);
    if(err < 0)
        throw KernelArgError(std::to_string(err));
}

void CLKernelObject::setOutputBuffer(std::shared_ptr<Buffer> buffer) {
    auto clBuffer = (CLBuffer *) buffer.get();
    auto clMem = clBuffer->clMem();
    const unsigned OutputBufferIndex = 1;

    int err = clSetKernelArg(_kernel, OutputBufferIndex, sizeof(clMem), &clMem);
    if(err < 0)
        throw KernelArgError(std::to_string(err));
}

void CLKernelObject::setPrimitiveArg(const std::string &name,
        size_t size, const void *host) {
    int err = clSetKernelArg(_kernel, _params[name], size, host);
    if(err < 0)
        throw KernelArgError(std::to_string(err));
}

void CLKernelObject::setArg(const std::string &name, std::shared_ptr<Buffer> buffer) {
    auto clBuffer = (CLBuffer *) buffer.get();
    auto clMem = clBuffer->clMem();

    int err = clSetKernelArg(_kernel, _params[name], sizeof(clMem), &clMem);
    if(err < 0)
        throw KernelArgError(std::to_string(err));
}

void CLKernelObject::setArg(const std::string &name, jchar primitive) {
    setPrimitiveArg(name, sizeof(primitive), &primitive);
}

void CLKernelObject::setArg(const std::string &name, jbyte primitive) {
    setPrimitiveArg(name, sizeof(primitive), &primitive);
}

void CLKernelObject::setArg(const std::string &name, jshort primitive) {
    setPrimitiveArg(name, sizeof(primitive), &primitive);
}

void CLKernelObject::setArg(const std::string &name, jint primitive) {
    setPrimitiveArg(name, sizeof(primitive), &primitive);
}

void CLKernelObject::setArg(const std::string &name, jfloat primitive) {
    setPrimitiveArg(name, sizeof(primitive), &primitive);
}

