/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include "org_parallelme_runtime_ParallelMERuntimeJNIWrapper.hpp"
#include "error.h"
#include <stdexcept>

JavaVM *gJvm;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
	gJvm = jvm;
	return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_init(JNIEnv *env, jobject a) {
	try {
		return reinterpret_cast<jlong>(new ParallelMERuntime(gJvm, env));
	} catch (std::runtime_error &e) {
		stop_if(true, "Error in ParallelME runtime initialization: %s", e.what());
	}
}

JNIEXPORT void JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_cleanUp(JNIEnv *env, jobject a, jlong parallelMERuntime) {
	auto foo = reinterpret_cast<ParallelMERuntime *>(parallelMERuntime);
	delete foo;
}

JNIEXPORT void JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_waitFinish(JNIEnv *env, jobject a, jlong parallelMERuntime) {
	auto foo = reinterpret_cast<ParallelMERuntime *>(parallelMERuntime);
	try {
		foo->waitFinish();
	} catch (std::runtime_error &e) {
		stop_if(true, "Error waiting for ParallelME runtime finish: %s", e.what());
	}
}

JNIEXPORT void JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_toFloat(JNIEnv *env, jobject a, jlong parallelMERuntime, jint inputBufferId, jint outputBufferId, jint workSize) {
	auto foo = reinterpret_cast<ParallelMERuntime *>(parallelMERuntime);
	std::list<ExtraArgument> extra_args;
	std::list<int> input_args;
	std::list<int> output_args;
	input_args.push_back(inputBufferId);
	output_args.push_back(outputBufferId);
	try {
		foo->addKernel(input_args,output_args, "toFloat", extra_args, workSize);
	} catch (std::runtime_error &e) {
		stop_if(true, "Error in ParallelME toFloat: %s", e.what());
	}
}

JNIEXPORT void JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_toBitmap(JNIEnv *env, jobject a, jlong parallelMERuntime, jint inputBufferId, jint outputBufferId, jint workSize) {
	auto foo = reinterpret_cast<ParallelMERuntime *>(parallelMERuntime);
	std::list<ExtraArgument> extra_args;
	std::list<int> input_args;
	std::list<int> output_args;
	input_args.push_back(inputBufferId);
	output_args.push_back(outputBufferId);
	try {
		foo->addKernel(input_args,output_args, "toBitmap", extra_args, workSize);
	} catch (std::runtime_error &e) {
		stop_if(true, "Error in ParallelME toBitmap: %s", e.what());
	}
}

JNIEXPORT jint JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_createByteAllocation(JNIEnv *env, jobject a, jlong parallelMERuntime, jbyteArray data, jint elements) {
	auto foo = reinterpret_cast<ParallelMERuntime *>(parallelMERuntime);
	try {
		return foo->createByteAllocation(env, data, elements);
	} catch (std::runtime_error &e) {
		stop_if(true, "Error in ParallelME createByteAllocation: %s", e.what());
	}
}

JNIEXPORT jint JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_createBitmapAllocation(JNIEnv *env, jobject a, jlong parallelMERuntime, jobject data, jint elements) {
	auto foo = reinterpret_cast<ParallelMERuntime *>(parallelMERuntime);
	try {
		return foo->createBitmapAllocation(env, data, elements);
	} catch (std::runtime_error &e) {
		stop_if(true, "Error in ParallelME createBitmapAllocation: %s", e.what());
	}
}

JNIEXPORT jint JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_createFloatAllocation(JNIEnv *env, jobject a, jlong parallelMERuntime, jfloatArray data, jint elements) {
	auto foo = reinterpret_cast<ParallelMERuntime *>(parallelMERuntime);
	try {
		return foo->createFloatAllocation(env, data, elements);
	} catch (std::runtime_error &e) {
		stop_if(true, "Error in ParallelME createFloatAllocation: %s", e.what());
	}
}
