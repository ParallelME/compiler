/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#include <jni.h>

#ifndef _Included_org_parallelme_ParallelMERuntimeJNIWrapper
#define _Included_org_parallelme_ParallelMERuntimeJNIWrapper
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_init(JNIEnv *, jobject);

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_cleanUp(JNIEnv *, jobject, jlong);

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_createHDRImage(JNIEnv *, jobject, jlong, jbyteArray, jint, jint);

JNIEXPORT void JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_toBitmap(JNIEnv *, jobject, jlong, jobject);

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_getHeight(JNIEnv *, jobject, jlong);

JNIEXPORT jint JNICALL Java_org_parallelme_ParallelMERuntimeJNIWrapper_getWidth(JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
