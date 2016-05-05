#ifndef RS_RSPROGRAM_HPP
#define RS_RSPROGRAM_HPP

#include "../Program.hpp"
#include <jni.h>

class Runtime;

/**
 * Implementation of the Program class for RenderScript. Use it to specify your
 * RenderScript ScriptC class to be used by the Runtiem.
 */
class RSProgram : public Program {
    ProgramObjectMap _programObjects;

public:
    /**
     * Creates the RenderScript program.
     * @param runtime The Runtime instance.
     * @param scriptcName The ScriptC class name that contains all the kernels.
     */
    RSProgram(std::shared_ptr<Runtime> runtime, jstring scriptcName);

    ~RSProgram();

    inline ProgramObjectMap &programObjects() {
         return _programObjects;
    }
};

#endif // !RS_RSPROGRAM_HPP
