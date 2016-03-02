#include "Device.hpp"
#include <string>

Device::Device(cl_device_id device, unsigned id) : _clDevice(device),
        _clContext(nullptr), _clQueue(nullptr), _id(id), _type(CPU){
    int err;

    cl_device_type clType;
    err = clGetDeviceInfo(_clDevice, CL_DEVICE_TYPE, sizeof(cl_device_type),
            &clType, nullptr);
    if(err < 0)
        throw DeviceConstructionError(std::to_string(err));

    if(clType & CL_DEVICE_TYPE_CPU)
        _type = CPU;
    else if(clType & CL_DEVICE_TYPE_GPU)
        _type = GPU;
    else if(clType & CL_DEVICE_TYPE_ACCELERATOR)
        _type = Accelerator;
    else
        throw DeviceConstructionError("Invalid device type.");

    _clContext = clCreateContext(NULL, 1, &_clDevice, NULL, NULL, &err);
    if(err < 0)
        throw DeviceConstructionError(std::to_string(err));

    _clQueue = clCreateCommandQueue(_clContext, _clDevice, 0, &err);
    if(err < 0)
        throw DeviceConstructionError(std::to_string(err));
}

Device::~Device() {
    clReleaseCommandQueue(_clQueue);
    clReleaseContext(_clContext);
    //clReleaseDevice(_clDevice); // Not supported on OpenCL 1.1.
}

