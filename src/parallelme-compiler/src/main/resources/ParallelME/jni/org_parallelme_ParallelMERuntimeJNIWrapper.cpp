/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include "org_parallelme_ParallelMERuntimeJNIWrapper.hpp"
#include <memory>
#include <stdexcept>
#include <android/log.h>
#include <parallelme/ParallelME.hpp>
#include <parallelme/SchedulerHEFT.hpp>
#include "ParallelMEData.h"
#include "kernels.h"

using namespace parallelme;

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_init(JNIEnv *env, jobject self) {
	try {
		JavaVM *jvm;
		env->GetJavaVM(&jvm);
		auto ptr = new ParallelMEData();
		ptr->runtime = std::make_shared<Runtime>(jvm, std::make_shared<SchedulerHEFT>());
		ptr->program = std::make_shared<Program>(ptr->runtime, kernel);
		return (jlong) ptr;
	} catch (const std::runtime_error &e) {
		__android_log_print(ANDROID_LOG_ERROR, "Error in ParallelME runtime initialization: %s",
			"%s", e.what());
	}
	return 0;
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_cleanUp(JNIEnv *env, jobject self, jlong lPtr) {
	if(lPtr)
		delete (ParallelMEData *) lPtr;
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_createHDRImage(JNIEnv *env, jobject self, jlong lPtr, jbyteArray data, jint width, jint height) {
	auto ptr = (ParallelMEData *) lPtr;
	ptr->width = width;
	ptr->height = height;
	ptr->workSize = width * height;

	// Num elements * items per element * size of item
	ptr->inputBuffer = std::make_shared<Buffer>(Buffer::sizeGenerator(ptr->workSize, Buffer::RGBA));
	ptr->inputBuffer->copyFromJNI(env, data);
	ptr->outputBuffer = std::make_shared<Buffer>(Buffer::sizeGenerator(ptr->workSize, Buffer::FLOAT4));

	auto task = std::make_unique<Task>(ptr->program);
	task->addKernel("toFloat");
	task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
		kernelHash["toFloat"]
			->setArg(0, ptr->inputBuffer)
			->setArg(1, ptr->outputBuffer)
			->setWorkSize(ptr->workSize);
	});
	ptr->runtime->submitTask(std::move(task));
	ptr->runtime->finish();
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_toBitmap(JNIEnv *env, jobject self, jlong lPtr, jobject bitmap) {
	auto ptr = (ParallelMEData *) lPtr;
	auto task = std::make_unique<Task>(ptr->program);
	task->addKernel("toBitmap");
	task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
		kernelHash["toBitmap"]
			->setArg(0, ptr->outputBuffer)
			->setArg(1, ptr->inputBuffer)
			->setWorkSize(ptr->workSize);
	});
	ptr->runtime->submitTask(std::move(task));
	ptr->runtime->finish();
	ptr->inputBuffer->copyToJNI(env, bitmap);
	ptr->inputBuffer.reset();
	ptr->outputBuffer.reset();
}

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_getHeight(JNIEnv *env, jobject self, jlong lPtr) {
	auto ptr = (ParallelMEData *) lPtr;
	return ptr->height;
}

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_getWidth(JNIEnv *env, jobject self, jlong lPtr) {
	auto ptr = (ParallelMEData *) lPtr;
	return ptr->width;
}
