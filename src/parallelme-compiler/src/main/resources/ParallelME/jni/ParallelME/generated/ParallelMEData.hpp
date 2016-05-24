/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELMEDATA_HPP
#define PARALLELMEDATA_HPP

#include <parallelme/ParallelME.hpp>

struct ParallelMERuntimeData {
    std::shared_ptr<parallelme::Runtime> runtime;
    std::shared_ptr<parallelme::Program> program;
};

struct ImageData {
    std::shared_ptr<parallelme::Buffer> inputBuffer, outputBuffer;
    jint width;
    jint height;
    jint workSize;
};

struct ArrayData {
    std::shared_ptr<parallelme::Buffer> inputBuffer, outputBuffer;
    jint length;
    jint workSize;
};

#endif // !PARALLELMEDATA_HPP