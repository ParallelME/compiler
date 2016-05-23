/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include "org_parallelme_ParallelMERuntime.h"
#include <memory>
#include <stdexcept>
#include <android/log.h>
#include <parallelme/ParallelME.hpp>
#include <parallelme/ParallelMEData.h>
#include <parallelme/SchedulerHEFT.hpp>
#include "userKernels.h"

using namespace parallelme;

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_init(JNIEnv *env, jobject self) {
	jlong ret = 0;
    try {
        JavaVM *jvm;
        env->GetJavaVM(&jvm);
        auto runtimePtr = new ParallelMERuntimeData();
        runtimePtr->runtime = std::make_shared<Runtime>(jvm, std::make_shared<SchedulerHEFT>());
        runtimePtr->program = std::make_shared<Program>(runtimePtr->runtime, userKernels);
        ret = (jlong) runtimePtr;
    } catch (const std::runtime_error &e) {
        __android_log_print(ANDROID_LOG_ERROR, "ParallelME Runtime",
                "%s", e.what());
    }
    return ret;
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_cleanUp(JNIEnv *env, jobject self, jlong rtmPtr) {
	if (rtmPtr)
		delete (ParallelMERuntimeData *) rtmPtr;
}

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_createHDRImage(JNIEnv *env, jobject self, jlong rtmPtr, jbyteArray data, jint width, jint height) {
    auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;
	auto imagePtr = new ImageData();
    imagePtr->width = width;
    imagePtr->height = height;
    imagePtr->workSize = width * height;

    // Num elements * items per element * size of item
    imagePtr->inputBuffer = std::make_shared<Buffer>(Buffer::sizeGenerator(imagePtr->workSize, Buffer::RGBA));
    imagePtr->inputBuffer->copyFromJNI(env, data);
    imagePtr->outputBuffer = std::make_shared<Buffer>(Buffer::sizeGenerator(imagePtr->workSize, Buffer::FLOAT4));

    auto task = std::make_unique<Task>(runtimePtr->program);
    task->addKernel("toFloat");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["toFloat"]
            ->setArg(0, imagePtr->inputBuffer)
            ->setArg(1, imagePtr->outputBuffer)
            ->setWorkSize(imagePtr->workSize);
    });
    runtimePtr->runtime->submitTask(std::move(task));
    runtimePtr->runtime->finish();
	return (jlong) imagePtr;
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_toBitmapHDRImage(JNIEnv *env, jobject self, jlong rtmPtr, jlong imgPtr, jobject bitmap) {
    auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;
	auto imagePtr = (ImageData *) imgPtr;
	
    auto task = std::make_unique<Task>(runtimePtr->program);
    task->addKernel("toBitmapHDRImage");
    task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
        kernelHash["toBitmapHDRImage"]
            ->setArg(0, imagePtr->outputBuffer)
            ->setArg(1, imagePtr->inputBuffer)
            ->setWorkSize(imagePtr->workSize);
    });
    runtimePtr->runtime->submitTask(std::move(task));
    runtimePtr->runtime->finish();
    imagePtr->inputBuffer->copyToJNI(env, bitmap);

    imagePtr->inputBuffer.reset();
    imagePtr->outputBuffer.reset();
}

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntime_getHeight(JNIEnv *env, jobject self, jlong imgPtr) {
	auto imagePtr = (ImageData *) imgPtr;
    return imgPtr->height;
}

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntime_getWidth(JNIEnv *env, jobject self, jlong imgPtr) {
	auto imagePtr = (ImageData *) imgPtr;
    return imgPtr->width;
}
