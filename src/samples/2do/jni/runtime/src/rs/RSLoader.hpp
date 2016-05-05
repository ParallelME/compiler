#ifndef RS_RSLOADER_HPP
#define RS_RSLOADER_HPP

#include "../Loader.hpp"
#include <Device.hpp>

class RSLoader : public Loader {
    DeviceVector _devices;

public:
    /**
     * To load RenderScript, the JavaVM and the android context are needed.
     * @see Runtime
     */
    RSLoader(JavaVM *jvm, jobject androidContext);
    ~RSLoader();

    inline const DeviceVector &devices() {
        return _devices;
    }
};

#endif // !RS_RSLOADER_HPP
