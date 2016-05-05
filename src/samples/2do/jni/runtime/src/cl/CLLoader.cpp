#include <string>
#include <Device.hpp>
#include "CLLoader.hpp"
#include "CLDevice.hpp"
#include "../util/error.h"

CLLoader::CLLoader(JavaVM *jvm) {
    if(!dynLoadOpenCL()) {
        // Fail silently, as we'll just have no devices to add to the runtime.
        return;
    }

    int err;

    // Get the number of platforms.
    unsigned numPlatforms;
    err = clGetPlatformIDs(0, nullptr, &numPlatforms);
    if(err < 0)
        throw LoaderError(std::to_string(err));

    // Get the platforms.
    auto platforms =
        std::unique_ptr<cl_platform_id []>{new cl_platform_id[numPlatforms]};
    err = clGetPlatformIDs(numPlatforms, platforms.get(), nullptr);
    if(err < 0)
        throw LoaderError(std::to_string(err));

    // Initialize the devices for each platform.
    for(unsigned i = 0; i < numPlatforms; ++i) {
        unsigned numDevices;
        err = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, 0, nullptr,
                &numDevices);
        if(err < 0)
            throw LoaderError(std::to_string(err));

        std::unique_ptr<cl_device_id []> devices{new cl_device_id[numDevices]};
        err = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, numDevices,
                devices.get(), nullptr);
        if(err < 0)
            throw LoaderError(std::to_string(err));

        for(unsigned j = 0; j < numDevices; ++j)
            _devices.push_back(std::make_shared<CLDevice>(devices[j],
                    findType(devices[j]), jvm));
    }
}

CLLoader::~CLLoader() {
    dynCloseOpenCL();
}

DeviceType CLLoader::findType(cl_device_id id) {
    int err;
    cl_device_type clType;

    err = clGetDeviceInfo(id, CL_DEVICE_TYPE, sizeof(cl_device_type), &clType,
            nullptr);
    if(err < 0)
        throw LoaderError(std::to_string(err));

    // Save the type of the device.
    DeviceType type;
    switch(clType) {
    case CL_DEVICE_TYPE_CPU:
        type = DeviceType::CPU;
        break;

    case CL_DEVICE_TYPE_GPU:
        type = DeviceType::GPU;
        break;

    case CL_DEVICE_TYPE_ACCELERATOR:
        type = DeviceType::Accelerator;
        break;

    case CL_DEVICE_TYPE_DEFAULT:
        type = DeviceType::CPU; // Assume CPU.
        break;

    default:
        throw LoaderError("Invalid device type.");
    }

    return type;
}

