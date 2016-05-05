#include "RSLoader.hpp"
#include "RSDevice.hpp"

RSLoader::RSLoader(JavaVM *jvm, jobject androidContext) {
    JNIEnv *env;
    int status = jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if(status != JNI_OK)
        throw std::runtime_error("failed to get the JNIEnv of the thread.");

    auto rs = env->FindClass("android/support/v8/renderscript/RenderScript");
    if(!rs)
        throw std::runtime_error("failed to get RenderScript class.");

    auto create = env->GetStaticMethodID(rs, "create",
            "(Landroid/content/Context;)Landroid/support/v8/renderscript/RenderScript;");
    if(!create)
        throw std::runtime_error("failed to get the create function.");

    auto rsLocalContext = env->CallStaticObjectMethod(rs, create, androidContext);
    if(env->ExceptionCheck())
        throw std::runtime_error("failed to call RenderScript.create().");

    _devices.push_back(std::make_shared<RSDevice>(jvm, rsLocalContext));
}

RSLoader::~RSLoader() = default;
