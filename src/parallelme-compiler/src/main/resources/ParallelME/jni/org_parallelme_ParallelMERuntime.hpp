/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <jni.h>

#ifndef _Included_org_parallelme_ParallelMERuntime
#define _Included_org_parallelme_ParallelMERuntime
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_init(JNIEnv *, jobject);

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_cleanUp(JNIEnv *, jobject, jlong);

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntime_createHDRImage(JNIEnv *, jobject, jlong, jbyteArray, jint, jint);

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntime_toBitmapHDRImage(JNIEnv *, jobject, jlong, jlong, jobject);

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntime_getHeight(JNIEnv *, jobject, jlong);

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntime_getWidth(JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
