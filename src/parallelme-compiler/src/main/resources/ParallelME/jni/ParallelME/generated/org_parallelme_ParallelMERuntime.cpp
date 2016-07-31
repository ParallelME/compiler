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
		runtimePtr->program = std::make_shared<Program>(runtimePtr->runtime, userKernels,
			"-Werror -cl-strict-aliasing -cl-mad-enable -cl-no-signed-zeros -cl-finite-math-only");
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

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_nativeCreateArray__II(JNIEnv *env, jobject self, jint length, jint typeNo) {
	auto arrayPtr = new ArrayData();
	arrayPtr->length = length;
	arrayPtr->workSize = length;
	if (typeNo == 1) {
		arrayPtr->buffer = std::make_shared<Buffer>(Buffer::sizeGenerator(arrayPtr->workSize, Buffer::SHORT));
	} else if (typeNo == 2) {
		arrayPtr->buffer = std::make_shared<Buffer>(Buffer::sizeGenerator(arrayPtr->workSize, Buffer::INT));
	} else if (typeNo == 3) {
		arrayPtr->buffer = std::make_shared<Buffer>(Buffer::sizeGenerator(arrayPtr->workSize, Buffer::FLOAT));
	} else {
		__android_log_print(ANDROID_LOG_ERROR, "ParallelME Runtime", "Type not supported for nativeCreateArray: %d", typeNo);
	}
	return (jlong) arrayPtr;
}

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_nativeCreateArray__IILjava_lang_Object_2(JNIEnv *env, jobject self, jint length, jint typeNo, jobject data) {
	auto arrayPtr = reinterpret_cast<ArrayData*>(Java_org_parallelme_ParallelMERuntime_nativeCreateArray__II(env, self, length, typeNo));
	jarray *arr = reinterpret_cast<jarray*>(&data);
	arrayPtr->buffer->setJArraySource(env, *arr);
	return (jlong) arrayPtr;
}

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_nativeToArray(JNIEnv *env, jobject self, jlong arrPtr, jobject data) {
	auto arrayPtr = (ArrayData *) arrPtr;
	jarray *arr = reinterpret_cast<jarray*>(&data);
	arrayPtr->buffer->copyToJArray(env, *arr);
	delete arrayPtr;
}

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_nativeCreateBitmapImage(JNIEnv *env, jobject self, jlong rtmPtr, jobject data, jint width, jint height) {
	auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;
	auto imagePtr = new ImageData();
	imagePtr->width = width;
	imagePtr->height = height;
	imagePtr->workSize = width * height;

	// Num elements * items per element * size of item
	imagePtr->inputBuffer = std::make_shared<Buffer>(Buffer::sizeGenerator(imagePtr->workSize, Buffer::CHAR4));
	imagePtr->inputBuffer->setAndroidBitmapSource(env, data);
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

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_nativeToBitmapBitmapImage(JNIEnv *env, jobject self, jlong rtmPtr, jlong imgPtr, jobject bitmap) {
	auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;
	auto imagePtr = (ImageData *) imgPtr;

	auto task = std::make_unique<Task>(runtimePtr->program);
	task->addKernel("toBitmapBitmapImage");
	task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {
		kernelHash["toBitmapBitmapImage"]
			->setArg(0, imagePtr->outputBuffer)
			->setArg(1, imagePtr->inputBuffer)
			->setWorkSize(imagePtr->workSize);
	});
	runtimePtr->runtime->submitTask(std::move(task));
	runtimePtr->runtime->finish();
	imagePtr->inputBuffer->copyToAndroidBitmap(env, bitmap);

	delete imagePtr;
}

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_nativeCreateHDRImage(JNIEnv *env, jobject self, jlong rtmPtr, jbyteArray data, jint width, jint height) {
	auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;
	auto imagePtr = new ImageData();
	imagePtr->width = width;
	imagePtr->height = height;
	imagePtr->workSize = width * height;

	// Num elements * items per element * size of item
	imagePtr->inputBuffer = std::make_shared<Buffer>(Buffer::sizeGenerator(imagePtr->workSize, Buffer::RGBA));
	imagePtr->inputBuffer->setJArraySource(env, data);
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
	imagePtr->inputBuffer->copyToAndroidBitmap(env, bitmap);

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

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntime_nativeGetLength(JNIEnv *env, jobject self, jlong arrPtr) {
	auto arrayPtr = (ArrayData *) arrPtr;
	return arrayPtr->length;
}