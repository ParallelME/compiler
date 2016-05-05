#ifndef RS_RSDEVICE_HPP
#define RS_RSDEVICE_HPP

#include <Device.hpp>
#include <jni.h>

class RSDevice : public Device {

public:
    /**
     * Creates a RenderScript device.
     * @param jvm The JavaVM pointer.
     * @param rsLocalContext Local reference to the RenderScript Context. Will
     * be saved as a new global reference.
     */
    RSDevice(JavaVM *jvm, jobject rsLocalContext);

    ~RSDevice();

    inline void finish() {
        // Do nothing.
    }

    std::shared_ptr<Buffer> createBuffer(Buffer::ElementType type, int numX,
            int numY = 1, int numZ = 1);

    /// Constructs a new Element class, given its static function name.
    jobject newElement(JNIEnv *env, const char *elementFuncName);

    /// Constructs a Type class, given its element.
    jobject newType(JNIEnv *env, jobject element, int numX, int numY, int numZ);

    /// Constructs a new Allocation class, given its Type.
    jobject newAllocation(JNIEnv *env, jobject rsType);

    /// Returns a reference to the Allocation class, that shouldn't be stored.
    inline jclass allocationClass() {
        return _allocationC;
    }

    /// Returns the global ref to the RS context.
    inline jobject rsContext() {
        return _rsContext;
    }

private:
    jobject _rsContext;
    jclass _typeBuilderC;
    jmethodID _typeBuilderInit;
    jmethodID _typeBuilderSetX;
    jmethodID _typeBuilderSetY;
    jmethodID _typeBuilderSetZ;
    jmethodID _typeBuilderCreate;
    jclass _elementC;
    jclass _allocationC;
    jmethodID _allocationCreateTyped;
};

#endif // !RS_RSDEVICE_HPP
