#ifndef UTIL_ERROR_H
#define UTIL_ERROR_H

#include "platform.h"

// Define this to replace the program name printed in logcat on android.
#ifndef PROGRAM_NAME
#   define PROGRAM_NAME "ParallelME Runtime"
#endif

#ifdef RUNTIME_ANDROID // use __android_log_print
#   include <android/log.h>
#   define ERROR_PRINTER_FUNC(...) __android_log_print(ANDROID_LOG_ERROR, PROGRAM_NAME, __VA_ARGS__)
#else // use fprintf
#   include <stdio.h>
#   define ERROR_PRINTER_FUNC(...) fprintf(stderr, __VA_ARGS__)
#endif

/**
 * Prints an error message in the same style as printf() but to the correct
 * error stream (logcat on android, stderr to other systems).
 */
#define printError(...) do { ERROR_PRINTER_FUNC("%s:%d:%s: ", __FILE__, __LINE__, __func__); ERROR_PRINTER_FUNC(__VA_ARGS__); ERROR_PRINTER_FUNC("\n"); fflush(stdout); } while(0)

#include <stdlib.h>

/**
 * Stops the program if the condition given is false.
 * You can write an error message in the same style as printf(), but to stderr.
 */
#define stop_if(cond, ...) do { if(cond) { printError(__VA_ARGS__); exit(1); } } while(0)

/**
 * Returns (for void-returning functions) if the condition given is false.
 * You can write an error message in the same style as printf(), but to stderr.
 */
#define return_if(cond, ...) do { if(cond) { printError(__VA_ARGS__); return; } } while(0)

#endif // !UTIL_ERROR_H
