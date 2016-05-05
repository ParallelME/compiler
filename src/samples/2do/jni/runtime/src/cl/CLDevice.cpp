#include "CLDevice.hpp"
#include "CLBuffer.hpp"
#include <string>

CLDevice::CLDevice(cl_device_id device, DeviceType type, JavaVM *jvm)
        : Device(OpenCL, type, jvm), _device(device), _context(nullptr),
        _queue(nullptr) {
    int err;

    _context = clCreateContext(nullptr, 1, &_device, nullptr, nullptr, &err);
    if(err < 0)
        throw DeviceConstructionError(std::to_string(err));

    _queue = clCreateCommandQueue(_context, _device, 0, &err);
    if(err < 0)
        throw DeviceConstructionError(std::to_string(err));
}

CLDevice::~CLDevice() {
    clReleaseCommandQueue(_queue);
    clReleaseContext(_context);
}

std::shared_ptr<Buffer> CLDevice::createBuffer(Buffer::ElementType type,
        int numX, int numY, int numZ) {
    return std::shared_ptr<Buffer>((Buffer *) new CLBuffer(this, type, numX,
                numY, numZ));
}

