#include <rs/RSProgram.hpp>
#include <Runtime.hpp>
#include "RSDevice.hpp"
#include "RSProgramObject.hpp"

RSProgram::RSProgram(std::shared_ptr<Runtime> runtime, jstring scriptcName) {
    for(auto &device : runtime->devices()) {
        if(device->platform() != RenderScript)
            continue;

        auto rsDev = (RSDevice *) device.get();
        auto ctx = rsDev->rsContext();
        auto env = rsDev->jenv();

        auto name = env->GetStringUTFChars(scriptcName, nullptr);
        if(!name)
            throw std::runtime_error("Failed to get name string.");

        auto scriptcC = env->FindClass(name);
        if(!scriptcC)
            throw std::runtime_error("Failed to get the ScriptC_* class.");

        env->ReleaseStringUTFChars(scriptcName, name);

        auto initM = env->GetMethodID(scriptcC, "<init>",
                "(Landroid/support/v8/renderscript/RenderScript;)V");
        if(!initM)
            throw std::runtime_error("Failed to get the ScriptC_* constructor.");

        auto scriptc = env->NewObject(scriptcC, initM, ctx);
        if(!scriptc)
            throw std::runtime_error("Failed to create a new ScriptC_* class.");

        auto obj = std::make_shared<RSProgramObject>(device, scriptc);

        // Only add the program object if it was created successfully.
        if(obj->valid())
            _programObjects.insert(ProgramObjectPair(device->id(), obj));
    }
}

RSProgram::~RSProgram() = default;
