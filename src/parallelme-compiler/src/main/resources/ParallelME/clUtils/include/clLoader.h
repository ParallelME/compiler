#ifndef CLLOADER_H
#define CLLOADER_H

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Load and use the OpenCL 1.1 API in a hardware-independent manner for Android.
 */

#include <CL/cl.h>

/**
 * Loads the OpenCL library of the current system and links all the functions
 * correctly.
 * On failure this function returns 0, in which case trying to use OpenCL. Else,
 * it returns 1.
 * will result in a segfault.
 */
int loadOpenCL();

/**
 * Cleans up the OpenCL library and closes it.
 */
void closeOpenCL();

#ifdef __cplusplus
}
#endif

#endif // !CLLOADER_H
