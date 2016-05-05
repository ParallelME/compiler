#ifndef RS_RSKERNELOBJECT_HPP
#define RS_RSKERNELOBJECT_HPP

#include "../KernelObject.hpp"
#include <jni.h>

class Device;

class RSKernelObject : public KernelObject {
    std::shared_ptr<Buffer> _inputBuffer;
    std::shared_ptr<Buffer> _outputBuffer;
    jobject _scriptc;
    jclass _scriptcC;
    jmethodID _scriptcForEach;
    jclass _launchOptionsC;
    jmethodID _launchOptionsInit;
    jmethodID _launchOptionsSetX;
    jmethodID _launchOptionsSetY;
    jmethodID _launchOptionsSetZ;

    /// Returns the jmethodID of the ScriptC class.
    jmethodID getScriptMethod(const std::string &prename, const char *sig);

    /// Calls the given method to set the primitive argument.
    template<typename T>
    void setPrimitiveArg(jmethodID method, T primitive) {
        auto env = device()->jenv();

        env->CallVoidMethod(_scriptc, method, primitive);
        if(env->ExceptionCheck())
            throw std::runtime_error("Failed to call ScriptC.set_* in RenderScript.");
    }

public:
    RSKernelObject(const std::string &name, std::shared_ptr<Device> device,
            jobject scriptc);

    ~RSKernelObject();

    void run(int numX, int numY, int numZ);

    inline void setInputBuffer(std::shared_ptr<Buffer> buffer) {
        _inputBuffer = buffer;
    }

    inline void setOutputBuffer(std::shared_ptr<Buffer> buffer) {
         _outputBuffer = buffer;
    }

    void setArg(const std::string &name, std::shared_ptr<Buffer> buffer);
    void setArg(const std::string &name, jchar primitive);
    void setArg(const std::string &name, jbyte primitive);
    void setArg(const std::string &name, jshort primitive);
    void setArg(const std::string &name, jint primitive);
    void setArg(const std::string &name, jfloat primitive);
};

#endif // !RS_RSKERNELOBJECT_HPP
