#include "RSDevice.hpp"
#include "RSBuffer.hpp"
#include <stdexcept>

RSDevice::RSDevice(JavaVM *jvm, jobject rsLocalContext)
        : Device(RenderScript, CPU, jvm) {
    auto env = jenv();

    _rsContext = env->NewGlobalRef(rsLocalContext);
    if(!_rsContext)
        throw std::runtime_error("failed to create a global java ref.");

    // Save Type.Builder
    auto localBuilder = env->FindClass("android/support/v8/renderscript/Type$Builder");
    if(!localBuilder)
        throw std::runtime_error("failed to get Type.Builder class.");

    _typeBuilderC = (jclass) env->NewGlobalRef(localBuilder);
    if(!_typeBuilderC)
        throw std::runtime_error("failed to create a global java ref.");

    _typeBuilderInit = env->GetMethodID(_typeBuilderC, "<init>",
            "(Landroid/support/v8/renderscript/RenderScript;"
            "Landroid/support/v8/renderscript/Element;)V");
    if(!_typeBuilderInit)
        throw std::runtime_error("failed to get the Type.Builder.<init> method");

    _typeBuilderSetX = env->GetMethodID(_typeBuilderC, "setX",
            "(I)Landroid/support/v8/renderscript/Type$Builder;");
    if(!_typeBuilderSetX)
        throw std::runtime_error("failed to get the Type.Builder.setX method");

    _typeBuilderSetY = env->GetMethodID(_typeBuilderC, "setY",
            "(I)Landroid/support/v8/renderscript/Type$Builder;");
    if(!_typeBuilderSetY)
        throw std::runtime_error("failed to get the Type.Builder.setY method");

    _typeBuilderSetZ = env->GetMethodID(_typeBuilderC, "setZ",
            "(I)Landroid/support/v8/renderscript/Type$Builder;");
    if(!_typeBuilderSetZ)
        throw std::runtime_error("failed to get the Type.Builder.setZ method");

    _typeBuilderCreate = env->GetMethodID(_typeBuilderC, "create",
            "()Landroid/support/v8/renderscript/Type;");
    if(!_typeBuilderCreate)
        throw std::runtime_error("failed to get the Type.Builder.create method");

    // Save element types.
    auto localElement = env->FindClass("android/support/v8/renderscript/Element");
    if(!localElement)
        throw std::runtime_error("failed to get the Element class.");

    _elementC = (jclass) env->NewGlobalRef(localElement);
    if(!_elementC)
        throw std::runtime_error("failed to get a global java ref.");

    auto localAllocation = env->FindClass("android/support/v8/renderscript/Allocation");
    if(!localAllocation)
        throw std::runtime_error("failed to get the Allocation class.");

    // Save allocation.
    _allocationC = (jclass) env->NewGlobalRef(localAllocation);
    if(!_allocationC)
        throw std::runtime_error("failed to get a global java ref.");

    _allocationCreateTyped = env->GetStaticMethodID(_allocationC, "createTyped",
            "(Landroid/support/v8/renderscript/RenderScript;"
            "Landroid/support/v8/renderscript/Type;)"
            "Landroid/support/v8/renderscript/Allocation;");
    if(!_allocationCreateTyped)
        throw std::runtime_error("failed to get the Allocation.createTyped function.");

    env->DeleteLocalRef(localBuilder);
    env->DeleteLocalRef(localElement);
    env->DeleteLocalRef(localAllocation);
}

RSDevice::~RSDevice() {
    auto env = jenv();
    env->DeleteGlobalRef(_rsContext);
    _rsContext = nullptr;

    env->DeleteGlobalRef(_typeBuilderC);
    _typeBuilderC = nullptr;

    env->DeleteGlobalRef(_elementC);
    _elementC = nullptr;

    env->DeleteGlobalRef(_allocationC);
    _allocationC = nullptr;
}

std::shared_ptr<Buffer> RSDevice::createBuffer(Buffer::ElementType type, int numX,
        int numY, int numZ) {
    return std::make_shared<RSBuffer>(this, type, numX, numY, numZ);
}

jobject RSDevice::newElement(JNIEnv *env, const char *elementFuncName) {
    auto func = env->GetStaticMethodID(_elementC, elementFuncName,
            "(Landroid/support/v8/renderscript/RenderScript;)"
            "Landroid/support/v8/renderscript/Element;");
    if(!func)
        throw std::runtime_error("Failed to get an Element creator.");

    auto obj = env->CallStaticObjectMethod(_elementC, func, _rsContext);
    if(!obj)
        throw std::runtime_error("Failed to get the Element instance.");

    return obj;
}

jobject RSDevice::newType(JNIEnv *env, jobject element, int numX, int numY,
        int numZ) {
    auto builder = env->NewObject(_typeBuilderC, _typeBuilderInit, _rsContext,
            element);
    if(!builder)
        throw std::runtime_error("Failed to create a Type.Builder.");

    env->CallObjectMethod(builder, _typeBuilderSetX, numX);
    if(env->ExceptionCheck())
        throw std::runtime_error("failed to call Type.Builder.setX().");

    env->CallObjectMethod(builder, _typeBuilderSetY, numY);
    if(env->ExceptionCheck())
        throw std::runtime_error("failed to call Type.Builder.setY().");

    env->CallObjectMethod(builder, _typeBuilderSetZ, numZ);
    if(env->ExceptionCheck())
        throw std::runtime_error("failed to call Type.Builder.setZ().");

    auto obj = env->CallObjectMethod(builder, _typeBuilderCreate);
    if(!obj)
        throw std::runtime_error("failed to create a Renderscript.Type class.");

    return obj;
}

jobject RSDevice::newAllocation(JNIEnv *env, jobject rsType) {
    auto alloc = env->CallStaticObjectMethod(_allocationC, _allocationCreateTyped,
            _rsContext, rsType);
    if(!alloc)
        throw std::runtime_error("failed to create a Renderscript.Allocation class.");

    return alloc;
}

