/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include "org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL.h"
#include <memory>
#include <stdexcept>
#include <android/log.h>
#include <parallelme/ParallelME.hpp>
#include <parallelme/SchedulerHEFT.hpp>
using namespace parallelme;

const char gParallelMEReinhardCompilerOperatorSource[] =
    "__kernel void to_float(__global uchar4 *gIn,           \n"
    "        __global float4 *gOut) {                       \n"
    "    int gid = get_global_id(0);                        \n"
    "    uchar4 in = gIn[gid];                              \n"
    "    float4 out;                                        \n"
    "                                                       \n"
    "    float f;                                           \n"
    "    if(in.s3 != 0) {                                   \n"
    "        f = ldexp(1.0f, (in.s3 & 0xFF) - (128 + 8));   \n"
    "        out.s0 = (in.s0 & 0xFF) * f;                   \n"
    "        out.s1 = (in.s1 & 0xFF) * f;                   \n"
    "        out.s2 = (in.s2 & 0xFF) * f;                   \n"
    "    }                                                  \n"
    "    else {                                             \n"
    "        out.s0 = 0.0f;                                 \n"
    "        out.s1 = 0.0f;                                 \n"
    "        out.s2 = 0.0f;                                 \n"
    "    }                                                  \n"
    "                                                       \n"
    "    gOut[gid] = out;                                   \n"
    "}                                                      \n"
    "                                                       \n"
    "__kernel void iterator1(__global float4 *gData) {      \n"
    "    int gid = get_global_id(0);                        \n"
    "    float4 pixel = gData[gid];                         \n"
    "                                                       \n"
    "    float result0, result1, result2;                   \n"
    "    float w;                                           \n"
    "                                                       \n"
    "    // These constants are the conversion coefs.       \n"
    "    result0 += 0.5141364f * pixel.s0;                  \n"
    "    result0 += 0.3238786f * pixel.s1;                  \n"
    "    result0 += 0.16036376f * pixel.s2;                 \n"
    "    result1 += 0.265068f * pixel.s0;                   \n"
    "    result1 += 0.67023428f * pixel.s1;                 \n"
    "    result1 += 0.06409157f * pixel.s2;                 \n"
    "    result2 += 0.0241188f * pixel.s0;                  \n"
    "    result2 += 0.1228178f * pixel.s1;                  \n"
    "    result2 += 0.84442666f * pixel.s2;                 \n"
    "    w = result0 + result1 + result2;                   \n"
    "                                                       \n"
    "    if(w > 0.0f) {                                     \n"
    "        pixel.s0 = result1;     // Y                   \n"
    "        pixel.s1 = result0 / w; // x                   \n"
    "        pixel.s2 = result1 / w; // y                   \n"
    "    }                                                  \n"
    "    else {                                             \n"
    "        pixel.s0 = pixel.s1 = pixel.s2 = 0.0f;         \n"
    "    }                                                  \n"
    "                                                       \n"
    "    gData[gid] = pixel;                                \n"
    "}                                                      \n"
    "                                                       \n"
    "__kernel void iterator2(__global float4 *gData,        \n"
    "        float sum, __global float *outSum,             \n"
    "        float max, __global float *outMax,             \n"
    "        int width, int height) {                       \n"
    "    for(int i = 0; i < height; ++i) {                  \n"
    "       for(int j = 0; j < width; ++j) {                \n"
    "            float4 pixel = gData[i * width + j];       \n"
    "            sum += log(0.00001f + pixel.s0);           \n"
    "                                                       \n"
    "            if(pixel.s0 > max)                         \n"
    "               max = pixel.s0;                         \n"
    "       }                                               \n"
    "    }                                                  \n"
    "                                                       \n"
    "    *outSum = sum;                                     \n"
    "    *outMax = max;                                     \n"
    "}                                                      \n"
    "                                                       \n"
    "__kernel void iterator3(__global float4 *gData,        \n"
    "        float scaleFactor, float lmax2) {              \n"
    "    int gid = get_global_id(0);                        \n"
    "    float4 pixel = gData[gid];                         \n"
    "                                                       \n"
    "    pixel.s0 *= scaleFactor;                           \n"
    "                                                       \n"
    "    pixel.s0 *= (1.0f + pixel.s0 / lmax2)              \n"
    "           / (1.0f + pixel.s0);                        \n"
    "                                                       \n"
    "    gData[gid] = pixel;                                \n"
    "}                                                      \n"
    "                                                       \n"
    "__kernel void iterator4(__global float4 *gPixels) {    \n"
    "    int gid = get_global_id(0);                        \n"
    "    float4 pixel = gPixels[gid];                       \n"
    "                                                       \n"
    "    float x, y, z, g, b;                               \n"
    "                                                       \n"
    "    y = pixel.s0;                                      \n"
    "    g = pixel.s1;                                      \n"
    "    b = pixel.s2;                                      \n"
    "                                                       \n"
    "    if(y > 0.0f && g > 0.0f && b > 0.0f) {             \n"
    "        x = g * y / b;                                 \n"
    "        z = x / g - x - y;                             \n"
    "    }                                                  \n"
    "    else {                                             \n"
    "        x = z = 0.0f;                                  \n"
    "    }                                                  \n"
    "                                                       \n"
    "    // These constants are the conversion coefs.       \n"
    "    pixel.s0 = pixel.s1 = pixel.s2 = 0.0f;             \n"
    "    pixel.s0 += 2.5651f * x;                           \n"
    "    pixel.s0 += -1.1665f * y;                          \n"
    "    pixel.s0 += -0.3986f * z;                          \n"
    "    pixel.s1 += -1.0217f * x;                          \n"
    "    pixel.s1 += 1.9777f * y;                           \n"
    "    pixel.s1 += 0.0439f * z;                           \n"
    "    pixel.s2 += 0.0753f * x;                           \n"
    "    pixel.s2 += -0.2543f * y;                          \n"
    "    pixel.s2 += 1.1892f * z;                           \n"
    "                                                       \n"
    "    gPixels[gid] = pixel;                              \n"
    "}                                                      \n"
    "                                                       \n"
    "__kernel void iterator5(__global float4 *gData,        \n"
    "        float power) {                                 \n"
    "    int gid = get_global_id(0);                        \n"
    "    float4 pixel = gData[gid];                         \n"
    "                                                       \n"
    "    if(pixel.s0 > 1.0f) pixel.s0 = 1.0f;               \n"
    "    if(pixel.s1 > 1.0f) pixel.s1 = 1.0f;               \n"
    "    if(pixel.s2 > 1.0f) pixel.s2 = 1.0f;               \n"
    "                                                       \n"
    "    pixel.s0 = pow(pixel.s0, power);                   \n"
    "    pixel.s1 = pow(pixel.s1, power);                   \n"
    "    pixel.s2 = pow(pixel.s2, power);                   \n"
    "                                                       \n"
    "    gData[gid] = pixel;                                \n"
    "}                                                      \n"
    "                                                       \n"
    "__kernel void to_bitmap(__global float4 *gIn,          \n"
    "        __global uchar4 *gOut) {                       \n"
    "    int gid = get_global_id(0);                        \n"
    "    float4 in = gIn[gid];                              \n"
    "    uchar4 out;                                        \n"
    "    out.x = (uchar) (255.0f * in.s0);                  \n"
    "    out.y = (uchar) (255.0f * in.s1);                  \n"
    "    out.z = (uchar) (255.0f * in.s2);                  \n"
    "    out.w = 255;                                       \n"
    "    gOut[gid] = out;                                   \n"
    "}                                                      \n";

struct ParallelMEReinhardCompilerOperator {
    std::shared_ptr<Runtime> runtime;
    std::shared_ptr<Program> program;
    std::shared_ptr<Buffer> imageIn, imageOut;
    jint width;
    jint height;
    jint workSize;
};

JNIEXPORT jlong JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeInit
        (JNIEnv *env, jobject self) {
    try {
        JavaVM *jvm;
        env->GetJavaVM(&jvm);

        auto ptr = new ParallelMEReinhardCompilerOperator();
        ptr->runtime = std::make_shared<Runtime>(jvm, std::make_shared<SchedulerHEFT>());
        ptr->program = std::make_shared<Program>(ptr->runtime,
                gParallelMEReinhardCompilerOperatorSource);

        return (jlong) ptr;
    }
    catch(const std::runtime_error &e) {
        __android_log_print(ANDROID_LOG_ERROR, "ParallelME Runtime",
                "%s", e.what());
    }

    return 0;
}

JNIEXPORT void JNICALL Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeCleanUp
(JNIEnv *env, jobject self, jlong lPtr) {
    if(lPtr)
        delete (ParallelMEReinhardCompilerOperator *) lPtr;
}

JNIEXPORT void JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeCreateHDRImage
        (JNIEnv *env, jobject self, jlong lPtr, jbyteArray data, jint width, jint height) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;
    ptr->width = width;
    ptr->height = height;
    ptr->workSize = width * height;


    // Num elements * items per element * size of item
    ptr->imageIn = std::make_shared<Buffer>(Buffer::sizeGenerator(ptr->workSize, Buffer::RGBA));
    ptr->imageIn->copyFromJNI(env, data);
    ptr->imageOut = std::make_shared<Buffer>(Buffer::sizeGenerator(ptr->workSize, Buffer::FLOAT4));

    auto task = std::make_unique<Task>(ptr->program);
    task->addKernel("to_float");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["to_float"]
            ->setArg(0, ptr->imageIn)
            ->setArg(1, ptr->imageOut)
            ->setWorkSize(ptr->workSize);
    });
    ptr->runtime->submitTask(std::move(task));
    ptr->runtime->finish();
}

JNIEXPORT void JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeToBitmap
        (JNIEnv *env, jobject self, jlong lPtr, jobject bitmap) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;

    auto task = std::make_unique<Task>(ptr->program);
    task->addKernel("to_bitmap");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["to_bitmap"]
            ->setArg(0, ptr->imageOut)
            ->setArg(1, ptr->imageIn)
            ->setWorkSize(ptr->workSize);
    });
    ptr->runtime->submitTask(std::move(task));
    ptr->runtime->finish();
    ptr->imageIn->copyToJNI(env, bitmap);

    ptr->imageIn.reset();
    ptr->imageOut.reset();
}

JNIEXPORT jint JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeGetHeight
        (JNIEnv *env, jobject self, jlong lPtr) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;
    return ptr->height;
}

JNIEXPORT jint JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeGetWidth
        (JNIEnv *env, jobject self, jlong lPtr) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;
    return ptr->width;
}

JNIEXPORT void JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeIterator1
        (JNIEnv *env, jobject self, jlong lPtr) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;
	// Codigo paralelo
    auto task = std::make_unique<Task>(ptr->program);
    task->addKernel("iterator1");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["iterator1"]
            ->setArg(0, ptr->imageOut)
            ->setWorkSize(ptr->workSize);
    });
    ptr->runtime->submitTask(std::move(task));
    ptr->runtime->finish();
}

JNIEXPORT void JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeIterator2
    (JNIEnv *env, jobject self, jlong lPtr, jfloat sum, jfloatArray outSum, jfloat max, jfloatArray outMax) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;

    auto sumBuffer = std::make_shared<Buffer>(sizeof(sum));
    auto maxBuffer = std::make_shared<Buffer>(sizeof(max));
	// Codigo sequencial
    auto task = std::make_unique<Task>(ptr->program, Task::Score(1.0f, 2.0f));
    task->addKernel("iterator2");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["iterator2"]
            ->setArg(0, ptr->imageOut)
            ->setArg(1, sum)
            ->setArg(2, sumBuffer)
            ->setArg(3, max)
            ->setArg(4, maxBuffer)
            ->setArg(5, ptr->width)
            ->setArg(6, ptr->height)
            ->setWorkSize(1);
    });
    ptr->runtime->submitTask(std::move(task));
    ptr->runtime->finish();

    sumBuffer->copyToJNI(env, outSum);
    maxBuffer->copyToJNI(env, outMax);
}

JNIEXPORT void JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeIterator3
        (JNIEnv *env, jobject self, jlong lPtr, jfloat scaleFactor, jfloat lmax2) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;

    auto task = std::make_unique<Task>(ptr->program);
    task->addKernel("iterator3");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["iterator3"]
            ->setArg(0, ptr->imageOut)
            ->setArg(1, scaleFactor)
            ->setArg(2, lmax2)
            ->setWorkSize(ptr->workSize);
    });
    ptr->runtime->submitTask(std::move(task));
    ptr->runtime->finish();
}

JNIEXPORT void JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeIterator4
        (JNIEnv *env, jobject self, jlong lPtr) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;

    auto task = std::make_unique<Task>(ptr->program);
    task->addKernel("iterator4");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["iterator4"]
            ->setArg(0, ptr->imageOut)
            ->setWorkSize(ptr->workSize);
    });
    ptr->runtime->submitTask(std::move(task));
    ptr->runtime->finish();
}

JNIEXPORT void JNICALL
Java_org_parallelme_samples_tonemapreinhard_ParallelMEReinhardCompilerOperatorCL_nativeIterator5
        (JNIEnv *env, jobject self, jlong lPtr, jfloat power) {
    auto ptr = (ParallelMEReinhardCompilerOperator *) lPtr;

    auto task = std::make_unique<Task>(ptr->program);
    task->addKernel("iterator5");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["iterator5"]
            ->setArg(0, ptr->imageOut)
            ->setArg(1, power)
            ->setWorkSize(ptr->workSize);
    });
    ptr->runtime->submitTask(std::move(task));
    ptr->runtime->finish();
}
