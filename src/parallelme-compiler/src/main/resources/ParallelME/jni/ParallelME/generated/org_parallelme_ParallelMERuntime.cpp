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
#include "ParallelMEData.hpp"
#include <parallelme/SchedulerHEFT.hpp>
#include "userKernels.hpp"

using namespace parallelme;

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_nativeInit
  (JNIEnv *env, jobject self) {
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

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_nativeCleanUpRuntime(JNIEnv *env, jobject self, jlong rtmPtr) {
	if (rtmPtr)
		delete (ParallelMERuntimeData *) rtmPtr;
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_nativeCleanUpImage(JNIEnv *env, jobject self, jlong varPtr) {
	if (varPtr)
		delete (ImageData *) varPtr;
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_nativeCleanUpArray(JNIEnv *env, jobject self, jlong varPtr) {
	if (varPtr)
		delete (ArrayData *) varPtr;
}

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_nativeCreateHDRImage(JNIEnv *env, jobject self, jlong rtmPtr, jbyteArray data, jint width, jint height) {
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
	task->addKernel("toFloatHDRImage");
	task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
		kernelHash["toFloatHDRImage"]
			->setArg(0, imagePtr->inputBuffer)
			->setArg(1, imagePtr->outputBuffer)
			->setWorkSize(imagePtr->workSize);
	});
	runtimePtr->runtime->submitTask(std::move(task));
	runtimePtr->runtime->finish();
	return (jlong) imagePtr;
}

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_nativeCreateBitmapImage(JNIEnv *env, jobject self, jlong rtmPtr, jobject data, jint width, jint height) {
	auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;
	auto imagePtr = new ImageData();
	imagePtr->width = width;
	imagePtr->height = height;
	imagePtr->workSize = width * height;

	// Num elements * items per element * size of item
	imagePtr->inputBuffer = std::make_shared<Buffer>(Buffer::sizeGenerator(imagePtr->workSize, Buffer::CHAR4));
	imagePtr->inputBuffer->copyFromJNI(env, data);
	imagePtr->outputBuffer = std::make_shared<Buffer>(Buffer::sizeGenerator(imagePtr->workSize, Buffer::FLOAT4));

	auto task = std::make_unique<Task>(runtimePtr->program);
	task->addKernel("toFloatBitmapImage");
	task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
		kernelHash["toFloatBitmapImage"]
			->setArg(0, imagePtr->inputBuffer)
			->setArg(1, imagePtr->outputBuffer)
			->setWorkSize(imagePtr->workSize);
	});
	runtimePtr->runtime->submitTask(std::move(task));
	runtimePtr->runtime->finish();
	return (jlong) imagePtr;
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_nativeToBitmapHDRImage(JNIEnv *env, jobject self, jlong rtmPtr, jlong imgPtr, jobject bitmap) {
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

	delete imagePtr;
}

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntime_nativeGetHeight(JNIEnv *env, jobject self, jlong imgPtr) {
	auto imagePtr = (ImageData *) imgPtr;
	return imagePtr->height;
}

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntime_nativeGetWidth(JNIEnv *env, jobject self, jlong imgPtr) {
	auto imagePtr = (ImageData *) imgPtr;
	return imagePtr->width;
}