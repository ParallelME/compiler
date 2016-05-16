/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef CL_DYNLOADER_DYNLOADER_H
#define CL_DYNLOADER_DYNLOADER_H

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Load and use the OpenCL 1.1 API in a hardware-independent manner for Android.
 */

#include "headers/cl.h"

/**
 * Loads the OpenCL library of the current system and links all the functions
 * correctly.
 * On failure this function returns 0, in which case trying to use OpenCL. Else,
 * it returns 1.
 * will result in a segfault.
 */
int dynLoadOpenCL();

/**
 * Cleans up the OpenCL library and closes it.
 */
void dynCloseOpenCL();

#ifdef __cplusplus
}
#endif

#endif // !CL_DYNLOADER_DYNLOADER_H
