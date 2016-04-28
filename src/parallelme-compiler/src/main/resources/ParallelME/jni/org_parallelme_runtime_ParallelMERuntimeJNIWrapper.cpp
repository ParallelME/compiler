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
	} catch(std::runtime_error &e) {
		stop_if(true, "Error on ParallelME runtime initialization: %s", e.what());
	}
}

JNIEXPORT jlong JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_cleanUp(JNIEnv *env, jobject a) {
	auto foo = reinterpret_cast<ParallelMERuntime *>(parallelMERuntime);
	delete foo;
}
