/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <string>
#include <parallelme/Device.hpp>
#include "dynloader/dynLoader.h"
using namespace parallelme;

Device::Device(_cl_device_id *clDevice) : _clDevice(clDevice), _clContext(nullptr),
        _clQueue(nullptr), _type(findType(clDevice)), _id(genID()) {
    int err;

    _clContext = clCreateContext(nullptr, 1, &_clDevice, nullptr, nullptr, &err);
    if(err < 0)
        throw DeviceConstructionError(std::to_string(err));

    _clQueue = clCreateCommandQueue(_clContext, _clDevice, 0, &err);
    if(err < 0)
        throw DeviceConstructionError(std::to_string(err));

}

Device::~Device() {
    if(_clQueue) {
        clReleaseCommandQueue(_clQueue);
        _clQueue = nullptr;
    }
    if(_clContext) {
        clReleaseContext(_clContext);
        _clContext = nullptr;
    }
}

void Device::finish() {
    int err = clFinish(_clQueue);
    if(err < 0)
        throw DeviceFinishError(std::to_string(err));
}

Device::Type Device::findType(_cl_device_id *clDevice) {
    int err;
    cl_device_type clType;

    err = clGetDeviceInfo(clDevice, CL_DEVICE_TYPE, sizeof(cl_device_type), &clType,
            nullptr);
    if(err < 0)
        throw DeviceConstructionError(std::to_string(err));

    // Save the type of the device.
    Type type;
    switch(clType) {
    case CL_DEVICE_TYPE_CPU: type = CPU; break;
    case CL_DEVICE_TYPE_GPU: type = GPU; break;
    case CL_DEVICE_TYPE_ACCELERATOR: type = Accelerator; break;
    case CL_DEVICE_TYPE_DEFAULT: type = CPU; break; // Assume CPU.
    default: throw DeviceConstructionError("Invalid device type.");
    }

    return type;
}
