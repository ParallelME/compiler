/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

struct ParallelMEData {
    std::shared_ptr<Runtime> runtime;
    std::shared_ptr<Program> program;
    std::shared_ptr<Buffer> inputBuffer, outputBuffer;
    jint width;
    jint height;
    jint workSize;
};
