/*                                                _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

#ifndef PARALLELME_UTIL_PLATFORM_H
#define PARALLELME_UTIL_PLATFORM_H

// This file defines the current platform the runtime is running on.

// Define if the Runtime is running on Android.
#if defined(ANDROID) || defined(__ANDROID__)
#   define PARALLELME_ANDROID
#endif


#endif // !PARALLELME_UTIL_PLATFORM_H
