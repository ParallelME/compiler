LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libParallelMERuntime
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_CFLAGS := -O3 -Wall -Wextra -Werror -Wno-unused-parameter -Wno-extern-c-compat
LOCAL_CPPFLAGS := -O3 -std=c++14 -fexceptions
LOCAL_CPP_FEATURES += exceptions
LOCAL_LDLIBS := -llog -ldl -ljnigraphics -latomic
LOCAL_SRC_FILES := src/Kernel.cpp src/Runtime.cpp src/Task.cpp \
	src/sched/SchedulerFCFS.cpp src/sched/SchedulerHEFT.cpp \
	src/sched/SchedulerPAMS.cpp src/cl/CLBuffer.cpp src/cl/CLDevice.cpp \
	src/cl/CLKernelObject.cpp src/cl/CLLoader.cpp src/cl/CLProgram.cpp \
	src/cl/CLProgramObject.cpp src/cl/CLKernelParams.cpp \
	src/cl/dynloader/dynLoader.c src/rs/RSBuffer.cpp src/rs/RSDevice.cpp \
	src/rs/RSKernelObject.cpp src/rs/RSLoader.cpp src/rs/RSProgram.cpp \
	src/rs/RSProgramObject.cpp
include $(BUILD_SHARED_LIBRARY)
