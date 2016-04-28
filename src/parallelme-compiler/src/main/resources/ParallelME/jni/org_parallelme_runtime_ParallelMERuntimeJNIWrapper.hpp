/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <jni.h>

#ifndef _Included_org_parallelme_runtime_ParallelMERuntimeJNIWrapper
#define _Included_org_parallelme_runtime_ParallelMERuntimeJNIWrapper
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *);

JNIEXPORT jlong JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_init(JNIEnv *, jobject);

JNIEXPORT jlong JNICALL Java_org_parallelme_runtime_ParallelMERuntimeJNIWrapper_cleanUp(JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
