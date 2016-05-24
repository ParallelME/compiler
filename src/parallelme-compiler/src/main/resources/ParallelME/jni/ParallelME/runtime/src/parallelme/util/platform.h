/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef UTIL_PLATFORM_H
#define UTIL_PLATFORM_H

// This file defines the current platform the runtime is running on.

// Define if the Runtime is running on Android.
#if defined(ANDROID) || defined(__ANDROID__)
#   define RUNTIME_ANDROID
#endif


#endif // !PLATFORM_H
