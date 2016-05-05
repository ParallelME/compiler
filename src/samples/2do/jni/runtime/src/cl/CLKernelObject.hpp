#ifndef CL_CLKERNELOBJECT_HPP
#define CL_CLKERNELOBJECT_HPP

#include <memory>
#include "../KernelObject.hpp"
#include "CLKernelParams.hpp"
#include "dynloader/dynLoader.h"

class Device;

class CLKernelObject : public KernelObject {
    cl_kernel _kernel;
    KernelParamHash _params;

    void setPrimitiveArg(const std::string &name, size_t size, const void *host);

public:
    CLKernelObject(const std::string &name, std::shared_ptr<Device> device,
            cl_program clProgram, const KernelParamHash &params);

    void run(int xDim, int yDim, int zDim);

    void setInputBuffer(std::shared_ptr<Buffer> buffer);

    void setOutputBuffer(std::shared_ptr<Buffer> buffer);

    void setArg(const std::string &name, std::shared_ptr<Buffer> buffer);
    void setArg(const std::string &name, jchar primitive);
    void setArg(const std::string &name, jbyte primitive);
    void setArg(const std::string &name, jshort primitive);
    void setArg(const std::string &name, jint primitive);
    void setArg(const std::string &name, jfloat primitive);
};

#endif // !CL_CLKERNELOBJECT_HPP
