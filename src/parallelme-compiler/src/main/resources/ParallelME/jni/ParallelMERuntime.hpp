//
// Created by guilherme on 3/30/16.
//

#ifndef RUNTIME_PARALLELMERUNTIME_H
#define RUNTIME_PARALLELMERUNTIME_H

#include <memory>
#include <vector>
#include <jni.h>
#include <iostream>
#include <list>
#include <map>
#include "runtime/ExtraArgument.hpp"
#include "runtime/Allocation.hpp"
#include "runtime/Device.hpp"

class Runtime;
class Program;
class Task;
class Buffer;
class KernelCall;

/**
 * Class responsible for the scheduled implementation of Eric Reinhard's
 * tonemapping algorithm.
 */
class ParallelMERuntime {
        std::shared_ptr <Runtime> _runtime;
        std::shared_ptr <Program> _program;
        JavaVM *_jvm;
        std::map<jint,std::list<KernelCall>> calls;
        std::map<jint,std::pair<std::string,jint>> outputPerKernel;
        std::map<jint,std::shared_ptr<Buffer>> bufferTable;
        int numberOfTasks;

        std::vector<Allocation> allocations;

        //jobject sumPointerReference;

public:
        ParallelMERuntime(JavaVM *jvm, JNIEnv *p_mainEnv);

        ~ParallelMERuntime();

        void run(JNIEnv* mainEnv, jbyteArray input_array, jint input_array_id, size_t imageBufferSize, jfloatArray
        output_array, size_t dataBufferSize);


        void run(JNIEnv* mainEnv, jbyteArray input_array, jint input_array_id, size_t imageBufferSize, jobject
        output_bitmap, size_t dataBufferSize, jint workSize, jfloat power);

        void run(JNIEnv* mainEnv, jint output_arg_id);


        void addKernel(std::list<int> input_args, std::list<int> output_args, std::string kernel_name,
                       std::list<ExtraArgument> extra_args, int workSize);
        void waitFinish();

        int createBooleanAllocation(JNIEnv* mainEnv, jbooleanArray input_array, int elements);
        int createByteAllocation(JNIEnv* mainEnv, jbyteArray input_array, int elements);
        int createCharAllocation(JNIEnv* mainEnv, jcharArray input_array, int elements);
        int createShortAllocation(JNIEnv* mainEnv, jshortArray input_array, int elements);
        int createIntAllocation(JNIEnv* mainEnv, jintArray input_array, int elements);
        int createLongAllocation(JNIEnv* mainEnv, jlongArray input_array, int elements);
        int createDoubleAllocation(JNIEnv* mainEnv, jdoubleArray input_array, int elements);
        int createFloatAllocation(JNIEnv* mainEnv, jfloatArray input_array, int elements);
        int createBitmapAllocation(JNIEnv* mainEnv, jobject input_array, int elements);

        std::shared_ptr<Buffer> createNewInputBuffer(JNIEnv *env, Device &device, jint argID);
        std::shared_ptr<Buffer> createNewOutputBuffer(JNIEnv *env, Device &device, jint argID);

        //TODO: Remove method below, dont forget about the cpp!
        void userLogAverage(JNIEnv* mainEnv, jfloatArray data, jfloatArray out_sum, jfloat base_sum, jint height, jint width);
};

class KernelCall{
public:
        std::string kernel_name;
        std::list<ExtraArgument> extra_args;
        std::list<int> input_args;
        std::list<int> output_args;
        int workSize;
};

#endif //RUNTIME_PARALLELMERUNTIME_H