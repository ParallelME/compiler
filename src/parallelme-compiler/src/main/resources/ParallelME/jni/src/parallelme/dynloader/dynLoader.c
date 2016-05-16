/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <dlfcn.h>
#include <stdio.h>
#include "../util/error.h"

// Import OpenCL but this time defining the functions.
#define CL_API_ENTRY
#include "dynLoader.h"

/// Pointer to the OpenCL library.
static void *clLib = NULL;

#define PRINT_ERROR(symbol) printError("Failed to load the OpenCL symbol: %s", _CL_STRINGIFY(symbol))
/// This macro loads the given symbol from the given handle.
#define loadSymbol(handle, symbol) do { if(!(symbol = dlsym((handle), _CL_STRINGIFY(symbol)))) { PRINT_ERROR(symbol); return 0; } } while(0)

/// Loads the OpenCL symbols. Returns 0 on failure and 1 on success.
static int loadSymbols() {

    /* cl.h symbols */
    loadSymbol(clLib, clGetPlatformIDs);
    loadSymbol(clLib, clGetPlatformInfo);
    loadSymbol(clLib, clGetDeviceIDs);
    loadSymbol(clLib, clGetDeviceInfo);
    loadSymbol(clLib, clCreateContext);
    loadSymbol(clLib, clCreateContextFromType);
    loadSymbol(clLib, clRetainContext);
    loadSymbol(clLib, clReleaseContext);
    loadSymbol(clLib, clGetContextInfo);
    loadSymbol(clLib, clCreateCommandQueue);
    loadSymbol(clLib, clRetainCommandQueue);
    loadSymbol(clLib, clReleaseCommandQueue);
    loadSymbol(clLib, clGetCommandQueueInfo);
#ifdef CL_USE_DEPRECATED_OPENCL_1_0_APIS
    loadSymbol(clLib, clSetCommandQueueProperty);
#endif /* CL_USE_DEPRECATED_OPENCL_1_0_APIS */
    loadSymbol(clLib, clCreateBuffer);
    loadSymbol(clLib, clCreateSubBuffer);
    loadSymbol(clLib, clCreateImage2D);
    loadSymbol(clLib, clCreateImage3D);
    loadSymbol(clLib, clRetainMemObject);
    loadSymbol(clLib, clReleaseMemObject);
    loadSymbol(clLib, clGetSupportedImageFormats);
    loadSymbol(clLib, clGetMemObjectInfo);
    loadSymbol(clLib, clGetImageInfo);
    loadSymbol(clLib, clSetMemObjectDestructorCallback);
    loadSymbol(clLib, clCreateSampler);
    loadSymbol(clLib, clRetainSampler);
    loadSymbol(clLib, clReleaseSampler);
    loadSymbol(clLib, clGetSamplerInfo);
    loadSymbol(clLib, clCreateProgramWithSource);
    loadSymbol(clLib, clCreateProgramWithBinary);
    loadSymbol(clLib, clRetainProgram);
    loadSymbol(clLib, clBuildProgram);
    loadSymbol(clLib, clUnloadCompiler);
    loadSymbol(clLib, clGetProgramInfo);
    loadSymbol(clLib, clGetProgramBuildInfo);
    loadSymbol(clLib, clCreateKernel);
    loadSymbol(clLib, clCreateKernelsInProgram);
    loadSymbol(clLib, clRetainKernel);
    loadSymbol(clLib, clReleaseKernel);
    loadSymbol(clLib, clSetKernelArg);
    loadSymbol(clLib, clGetKernelInfo);
    loadSymbol(clLib, clGetKernelWorkGroupInfo);
    loadSymbol(clLib, clWaitForEvents);
    loadSymbol(clLib, clGetEventInfo);
    loadSymbol(clLib, clCreateUserEvent);
    loadSymbol(clLib, clRetainEvent);
    loadSymbol(clLib, clReleaseEvent);
    loadSymbol(clLib, clSetUserEventStatus);
    loadSymbol(clLib, clSetEventCallback);
    loadSymbol(clLib, clGetEventProfilingInfo);
    loadSymbol(clLib, clFlush);
    loadSymbol(clLib, clFinish);
    loadSymbol(clLib, clEnqueueReadBuffer);
    loadSymbol(clLib, clEnqueueReadBufferRect);
    loadSymbol(clLib, clEnqueueWriteBuffer);
    loadSymbol(clLib, clEnqueueWriteBufferRect);
    loadSymbol(clLib, clEnqueueCopyBuffer);
    loadSymbol(clLib, clEnqueueCopyBufferRect);
    loadSymbol(clLib, clEnqueueReadImage);
    loadSymbol(clLib, clEnqueueWriteImage);
    loadSymbol(clLib, clEnqueueCopyImage);
    loadSymbol(clLib, clEnqueueCopyImageToBuffer);
    loadSymbol(clLib, clEnqueueCopyBufferToImage);
    loadSymbol(clLib, clEnqueueMapBuffer);
    loadSymbol(clLib, clEnqueueMapImage);
    loadSymbol(clLib, clEnqueueUnmapMemObject);
    loadSymbol(clLib, clEnqueueNDRangeKernel);
    loadSymbol(clLib, clEnqueueTask);
    loadSymbol(clLib, clEnqueueNativeKernel);
    loadSymbol(clLib, clEnqueueMarker);
    loadSymbol(clLib, clEnqueueWaitForEvents);
    loadSymbol(clLib, clEnqueueBarrier);
    loadSymbol(clLib, clGetExtensionFunctionAddress);

    return 1;
}

#undef loadSymbol
#undef PRINT_ERROR

/// Tries to load the OpenCL library. Returns 0 on failure and 1 on success.
static int openLibrary() {
    if((clLib = dlopen("/system/vendor/lib/libOpenCL.so", RTLD_LAZY))) return 1;
    if((clLib = dlopen("/system/lib/libOpenCL.so", RTLD_LAZY))) return 1;
    if((clLib = dlopen("/system/vendor/lib/egl/libGLES_mali.so", RTLD_LAZY))) return 1;
    if((clLib = dlopen("/system/lib/egl/libGLES_mali.so", RTLD_LAZY))) return 1;
    if((clLib = dlopen("/system/vendor/lib/libPVROCL.so", RTLD_LAZY))) return 1;
    if((clLib = dlopen("/usr/lib64/libOpenCL.so", RTLD_LAZY))) return 1;
    if((clLib = dlopen("/usr/lib/libOpenCL.so", RTLD_LAZY))) return 1;
    if((clLib = dlopen("libOpenCL.so", RTLD_LAZY))) return 1;

    return 0;
}

int dynLoadOpenCL() {
    if(clLib)
        return 1; // Already loaded.

    if(!openLibrary()) {
        printError("Failed to open the OpenCL library: %s", dlerror());
        return 0;
    }

    if(!loadSymbols())  {
        dlclose(clLib);
        clLib = NULL;
        return 0;
    }

    return 1;
}

void dynCloseOpenCL() {
    if(!clLib) return; // Not loaded.

    dlclose(clLib);
    clLib = NULL;
}

