#include "RSKernelObject.hpp"
#include "RSDevice.hpp"
#include "RSBuffer.hpp"
#include <string>

RSKernelObject::RSKernelObject(const std::string &name,
        std::shared_ptr<Device> dev, jobject scriptc)
        : KernelObject(dev), _inputBuffer(nullptr), _outputBuffer(nullptr) {
    auto rsdev = (RSDevice *) device();
    auto env = rsdev->jenv();

    _scriptc = env->NewGlobalRef(scriptc);
    if(!_scriptc)
        throw std::runtime_error("Failed to create a new global ref.");

    auto localScriptcC = env->GetObjectClass(_scriptc);
    if(!localScriptcC)
        throw std::runtime_error("Failed to get the ScriptC class.");

    _scriptcC = (jclass) env->NewGlobalRef(localScriptcC);
    if(!_scriptcC)
        throw std::runtime_error("Failed to create a new global ref.");

    auto localLaunch = env->FindClass("android/support/v8/renderscript/Script$LaunchOptions");
    if(!localLaunch)
        throw std::runtime_error("Failed to find the Script.LaunchOptions class.");

    _launchOptionsC = (jclass) env->NewGlobalRef(localLaunch);
    if(!_launchOptionsC)
        throw std::runtime_error("Failed to create a new global ref.");

    _launchOptionsInit = env->GetMethodID(_launchOptionsC, "<init>", "()V");
    if(!_launchOptionsInit)
        throw std::runtime_error("Failed to get the LaunchOptions constructor.");

    _launchOptionsSetX = env->GetMethodID(_launchOptionsC, "setX", "(II)"
            "Landroid/support/v8/renderscript/Script$LaunchOptions;");
    if(!_launchOptionsSetX)
        throw std::runtime_error("Failed to get the LaunchOptions.setX() method.");

    _launchOptionsSetY = env->GetMethodID(_launchOptionsC, "setY", "(II)"
            "Landroid/support/v8/renderscript/Script$LaunchOptions;");
    if(!_launchOptionsSetY)
        throw std::runtime_error("Failed to get the LaunchOptions.setY() method.");

    _launchOptionsSetZ = env->GetMethodID(_launchOptionsC, "setZ", "(II)"
            "Landroid/support/v8/renderscript/Script$LaunchOptions;");
    if(!_launchOptionsSetZ)
        throw std::runtime_error("Failed to get the LaunchOptions.setZ() method.");

    std::string func{"forEach_"};
    func += name;

    _scriptcForEach = env->GetMethodID(_scriptcC, func.c_str(),
            "(Landroid/support/v8/renderscript/Allocation;"
             "Landroid/support/v8/renderscript/Allocation;"
             "Landroid/support/v8/renderscript/Script$LaunchOptions;)V");
    if(!_scriptcForEach)
        throw std::runtime_error("Failed to get the forEach_* method.");

    env->DeleteLocalRef(localScriptcC);
    env->DeleteLocalRef(localLaunch);
}

RSKernelObject::~RSKernelObject() {
    auto rsdev = (RSDevice *) device();
    auto env = rsdev->jenv();

    env->DeleteGlobalRef(_scriptc);
    _scriptc = nullptr;

    env->DeleteGlobalRef(_scriptcC);
    _scriptcC = nullptr;

    env->DeleteGlobalRef(_launchOptionsC);
    _launchOptionsC = nullptr;
}

jmethodID RSKernelObject::getScriptMethod(const std::string &prename, const char *sig) {
    auto env = device()->jenv();
    std::string name = "set_" + prename;

    auto func = env->GetMethodID(_scriptcC, name.c_str(), sig);
    if(!func)
        throw std::runtime_error("Failed to get the ScriptC method.");

    return func;
}

void RSKernelObject::run(int numX, int numY, int numZ) {
    auto rsdev = (RSDevice *) device();
    auto env = rsdev->jenv();

    auto opts = env->NewObject(_launchOptionsC, _launchOptionsInit);
    if(!opts)
        throw std::runtime_error("Failed to create a new Script.LaunchOptions class.");

    env->CallObjectMethod(opts, _launchOptionsSetX, 0, numX);
    if(env->ExceptionCheck())
        throw std::runtime_error("Failed to call LaunchOptions.setX().");

    env->CallObjectMethod(opts, _launchOptionsSetY, 0, numY);
    if(env->ExceptionCheck())
        throw std::runtime_error("Failed to call LaunchOptions.setY().");

    env->CallObjectMethod(opts, _launchOptionsSetZ, 0, numZ);
    if(env->ExceptionCheck())
        throw std::runtime_error("Failed to call LaunchOptions.setZ().");

    auto input = (RSBuffer *) _inputBuffer.get();
    auto output = (RSBuffer *) _outputBuffer.get();
    env->CallVoidMethod(_scriptc, _scriptcForEach, input->allocation(),
            output->allocation(), opts);
    if(env->ExceptionCheck())
        throw std::runtime_error("Failed to call the ScriptC.forEach_* method.");

    env->DeleteLocalRef(opts);
}

void RSKernelObject::setArg(const std::string &name, std::shared_ptr<Buffer> buffer) {
    auto rsBuf = (RSBuffer *) buffer.get();
    setPrimitiveArg(getScriptMethod(name,
        "(Landroid/support/v8/renderscript/Allocation;)V"), rsBuf->allocation());
}

void RSKernelObject::setArg(const std::string &name, jchar primitive) {
    setPrimitiveArg(getScriptMethod(name, "(C)V"), primitive);
}

void RSKernelObject::setArg(const std::string &name, jbyte primitive) {
    setPrimitiveArg(getScriptMethod(name, "(B)V"), primitive);
}

void RSKernelObject::setArg(const std::string &name, jshort primitive) {
    setPrimitiveArg(getScriptMethod(name, "(S)V"), primitive);
}

void RSKernelObject::setArg(const std::string &name, jint primitive) {
    setPrimitiveArg(getScriptMethod(name, "(I)V"), primitive);
}

void RSKernelObject::setArg(const std::string &name, jfloat primitive) {
    setPrimitiveArg(getScriptMethod(name, "(F)V"), primitive);
}

