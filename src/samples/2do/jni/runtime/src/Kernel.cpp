#include <Kernel.hpp>
#include <Buffer.hpp>
#include <Device.hpp>
#include "ProgramObject.hpp"
#include "KernelObject.hpp"

Kernel::Kernel(const std::string &name, const ProgramObjectMap &programObjects)
        : _xDim(-1), _inputBuffer(nullptr), _outputBuffer(nullptr) {
    for(auto progObj : programObjects) {
        auto kernelObj = progObj.second->createKernelObject(name);

        _kernelObjects.insert(KernelObjectPair(progObj.first, std::move(kernelObj)));
    }
}

Kernel::~Kernel() = default;

Kernel *Kernel::setInputBuffer(std::shared_ptr<Buffer> buffer) {
    _kernelObjects[buffer->device()->id()]->setInputBuffer(buffer);

    if(_xDim == -1) {
        _xDim = buffer->numX();
        _yDim = buffer->numY();
        _zDim = buffer->numZ();
    }

    _inputBuffer = buffer;
    return this;
}

Kernel *Kernel::setOutputBuffer(std::shared_ptr<Buffer> buffer) {
    _kernelObjects[buffer->device()->id()]->setOutputBuffer(buffer);

    _outputBuffer = buffer;
    return this;
}

Kernel *Kernel::setArg(const std::string &name, std::shared_ptr<Buffer> buffer) {
    _kernelObjects[buffer->device()->id()]->setArg(name, buffer);

    return this;
}

Kernel *Kernel::setArg(const std::string &name, jchar primitive) {
    for(auto &obj : _kernelObjects)
        obj.second->setArg(name, primitive);

    return this;
}

Kernel *Kernel::setArg(const std::string &name, jbyte primitive) {
    for(auto &obj : _kernelObjects)
        obj.second->setArg(name, primitive);

    return this;
}

Kernel *Kernel::setArg(const std::string &name, jshort primitive) {
    for(auto &obj : _kernelObjects)
        obj.second->setArg(name, primitive);

    return this;
}

Kernel *Kernel::setArg(const std::string &name, jint primitive) {
    for(auto &obj : _kernelObjects)
        obj.second->setArg(name, primitive);

    return this;
}

Kernel *Kernel::setArg(const std::string &name, jfloat primitive) {
    for(auto &obj : _kernelObjects)
        obj.second->setArg(name, primitive);

    return this;
}

