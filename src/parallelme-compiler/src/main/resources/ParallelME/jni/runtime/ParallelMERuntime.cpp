//
// Created by guilherme and pedro on 3/30/16.
//

#include "ParallelMERuntime.hpp"
#include "../runtime/Runtime.hpp"
#include "../runtime/Program.hpp"
#include "../runtime/Buffer.hpp"
#include "../runtime/Task.hpp"
#include "../runtime/Kernel.hpp"
#include "../runtime/Device.hpp"
#include "../error.h"
#include "../runtime/Allocation.hpp"
#include "kernels.h"
#include <android/bitmap.h>

/*
 * TODO: change "tonemapSource" to "kernelsSource"
 */
ParallelMERuntime::ParallelMERuntime(JavaVM *jvm, JNIEnv *p_mainEnv) : _jvm(jvm) {
    _runtime = std::make_shared<Runtime>(_jvm);
    _program = std::make_shared<Program>(_runtime, tonemapSource,
                                         "-Werror -cl-strict-aliasing -cl-mad-enable -cl-no-signed-zeros -cl-finite-math-only");
    numberOfTasks = 0;
}

ParallelMERuntime::~ParallelMERuntime() {

}

int ParallelMERuntime::createBooleanAllocation(JNIEnv* mainEnv, jbooleanArray input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jbooleanArray>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_BOOLEAN, elements);
    allocations.push_back(allocation);
    return index;
}

int ParallelMERuntime::createByteAllocation(JNIEnv* mainEnv, jbyteArray input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jbyteArray>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_BYTE, elements);
    allocations.push_back(allocation);
    return index;
}

int ParallelMERuntime::createCharAllocation(JNIEnv* mainEnv, jcharArray input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jcharArray>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_CHAR, elements);
    allocations.push_back(allocation);
    return index;
}

int ParallelMERuntime::createShortAllocation(JNIEnv* mainEnv, jshortArray input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jshortArray>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_SHORT, elements);
    allocations.push_back(allocation);
    return index;
}

int ParallelMERuntime::createIntAllocation(JNIEnv* mainEnv, jintArray input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jintArray>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_INT, elements);
    allocations.push_back(allocation);
    return index;
}

int ParallelMERuntime::createLongAllocation(JNIEnv* mainEnv, jlongArray input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jlongArray>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_LONG, elements);
    allocations.push_back(allocation);
    return index;
}

int ParallelMERuntime::createDoubleAllocation(JNIEnv* mainEnv, jdoubleArray input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jdoubleArray>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_DOUBLE, elements);
    allocations.push_back(allocation);
    return index;
}

int ParallelMERuntime::createBitmapAllocation(JNIEnv* mainEnv, jobject input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jobject>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_BITMAP, elements);
    allocations.push_back(allocation);
    return index;
}

int ParallelMERuntime::createFloatAllocation(JNIEnv* mainEnv, jfloatArray input_array, int elements){
    int index = allocations.size(); //You can't remove elements from allocation list otherwise this method won't work
    Allocation allocation(reinterpret_cast<jfloatArray>(mainEnv->NewGlobalRef(input_array)), ALLOCATION_TYPE_FLOAT, elements);
    allocations.push_back(allocation);
    return index;
}

void ParallelMERuntime::addKernel(std::list<int> input_args, std::list<int> output_args, std::string kernel_name,
    std::list<ExtraArgument> extra_args, int workSize){
        KernelCall genericKernel;
        genericKernel.kernel_name = kernel_name;
        genericKernel.workSize = workSize;

        //Adding extra arguments to Kernel Call
        for (std::list<ExtraArgument>::iterator arg=extra_args.begin(); arg != extra_args.end(); ++arg){
            genericKernel.extra_args.push_back(*arg);
        }

        int taskID;

        //Adding input arguments to Kernel Call
        for (std::list<int>::iterator inArg=input_args.begin(); inArg !=
        input_args.end(); ++inArg){

            genericKernel.input_args.push_back(*inArg);
            std::map<jint,std::pair<std::string,jint>>::iterator ref;
            ref = outputPerKernel.find(*inArg);
            if(ref != outputPerKernel.end()){
                taskID = ref->second.second;
            }
            else{
                taskID = numberOfTasks + 1;
                numberOfTasks++;
            }
        }

        //Adding output arguments to Kernel Call
        for (std::list<int>::iterator outArg= output_args.begin(); outArg !=
        output_args.end(); ++outArg){
        genericKernel.output_args.push_back(*outArg);
        std::pair<std::string,jint> kernel_task(kernel_name,taskID);
        outputPerKernel.insert(std::pair<jint,std::pair<std::string,jint>>(*outArg,kernel_task));
        }


        if(calls.find(taskID) != calls.end()){
            calls.find(taskID)->second.push_back(genericKernel);
        }
        else{
            printError("DEBUG: Adding Kernel %s to Task %d", kernel_name.c_str(), taskID);
            std::list<KernelCall> kernelCallList;
            kernelCallList.push_back(genericKernel);
            calls.insert(
                    std::pair<jint,std::list<KernelCall>>(taskID,kernelCallList));
        }

} //End of ParallelMERuntime::addKernel

std::shared_ptr<Buffer> ParallelMERuntime::createNewOutputBuffer(JNIEnv *env, Device &device, jint argID){

    size_t bufferSize = allocations[argID].getSize();

    printError("DEBUG: Creating new output buffer of size %d", bufferSize);

    auto allocationBuffer = std::make_shared<Buffer>(device, Buffer::ReadWrite, bufferSize);

    printError("DEBUG: Created new output buffer of size %d", bufferSize);

    switch(allocations[argID].getType()){
        case ALLOCATION_TYPE_BYTE:{
            //Don't try to create a variable here. You can't.
            printError("DEBUG: Creating Byte Output Buffer");
            auto input = env->GetByteArrayElements(allocations[argID].getBytePointer(),nullptr);
            allocationBuffer->copyFrom(input, bufferSize);}
            //allocationBuffer->copyFrom(env->GetByteArrayElements(reinterpret_cast<jbyteArray>(allocations[argID].getPointer()), nullptr), bufferSize);
            break;
        case ALLOCATION_TYPE_BOOLEAN:{
            printError("DEBUG: Creating Boolean Output Buffer");
            auto input = env->GetBooleanArrayElements(allocations[argID].getBooleanPointer(),nullptr);
            allocationBuffer->copyFrom(input, bufferSize);}
            //allocationBuffer->copyFrom(env->GetBooleanArrayElements(reinterpret_cast<jbooleanArray>(allocations[argID].getPointer()), nullptr), bufferSize);
            break;
        case ALLOCATION_TYPE_CHAR:{
            printError("DEBUG: Creating Char Output Buffer");
            auto input = env->GetCharArrayElements(allocations[argID].getCharPointer(),nullptr);
            allocationBuffer->copyFrom(input, bufferSize);}
            //allocationBuffer->copyFrom(env->GetCharArrayElements(reinterpret_cast<jcharArray>(allocations[argID].getPointer()), nullptr), bufferSize);
            break;
        case ALLOCATION_TYPE_SHORT:{
            printError("DEBUG: Creating Short Output Buffer");
            auto input = env->GetShortArrayElements(allocations[argID].getShortPointer(),nullptr);
            allocationBuffer->copyFrom(input, bufferSize);}
            //allocationBuffer->copyFrom(env->GetShortArrayElements(reinterpret_cast<jshortArray>(allocations[argID].getPointer()), nullptr), bufferSize);
            break;
        case ALLOCATION_TYPE_INT:{
            printError("DEBUG: Creating Int Output Buffer");
            auto input = env->GetIntArrayElements(allocations[argID].getIntPointer(),nullptr);
            allocationBuffer->copyFrom(input, bufferSize);}
            //allocationBuffer->copyFrom(env->GetIntArrayElements(reinterpret_cast<jintArray>(allocations[argID].getPointer()), nullptr), bufferSize);
            break;
        case ALLOCATION_TYPE_LONG:{
            printError("DEBUG: Creating Long Output Buffer");
            auto input = env->GetLongArrayElements(allocations[argID].getLongPointer(),nullptr);
            allocationBuffer->copyFrom(input, bufferSize);}
            //allocationBuffer->copyFrom(env->GetLongArrayElements(reinterpret_cast<jlongArray>(allocations[argID].getPointer()), nullptr), bufferSize);
            break;
        case ALLOCATION_TYPE_FLOAT:{
            printError("DEBUG: Creating Float Output Buffer");
            auto input = env->GetFloatArrayElements(allocations[argID].getFloatPointer(),nullptr);
            allocationBuffer->copyFrom(input, bufferSize);
            //allocationBuffer->copyFrom(env->GetFloatArrayElements(reinterpret_cast<jfloatArray>(allocations[argID].getPointer()), nullptr), bufferSize);
            printError("DEBUG: Created Float Output Buffer");}
            break;
        case ALLOCATION_TYPE_DOUBLE:{
            auto input = env->GetDoubleArrayElements(allocations[argID].getDoublePointer(),nullptr);
            allocationBuffer->copyFrom(input, bufferSize);}
            //allocationBuffer->copyFrom(env->GetDoubleArrayElements(reinterpret_cast<jdoubleArray>(allocations[argID].getPointer()), nullptr), bufferSize);
            break;
        default:
            printError("DEBUG: FATAL ERROR 666");
            break;
    }

    //Insert new buffer into bufferTable
    bufferTable.insert(std::pair<jint,std::shared_ptr<Buffer>>(argID,allocationBuffer));

    return allocationBuffer;
}

std::shared_ptr<Buffer> ParallelMERuntime::createNewInputBuffer(JNIEnv *env, Device &device, jint argID){

    std::shared_ptr<Buffer> allocation = createNewOutputBuffer(env,device,argID);

    //env->ReleaseByteArrayElements(reinterpret_cast<jbyteArray>(allocations[argID].getPointer()), allocation, 0);
    //env->DeleteGlobalRef(allocations[argID].getPointer());

    return allocation;
}

//Method to test user_log_average kernel using just the Scheduler, not the Java Wrapper
void ParallelMERuntime::userLogAverage(JNIEnv* mainEnv, jfloatArray data, jfloatArray out_sum, jfloat base_sum, jint height, jint width){
    auto inputArray = reinterpret_cast<jfloatArray>(mainEnv->NewGlobalRef(data));
    auto output_sum = reinterpret_cast<jfloatArray>(mainEnv->NewGlobalRef(out_sum));

    auto task = std::make_unique<Task>(_program);
    task->addKernel("user_log_average");

    task->setConfigFunction([=] (Device &device, Task::KernelHash &kernelHash) {
            JNIEnv *env;
            int status = _jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
            if(status != JNI_OK)
                throw std::runtime_error("failed to get the JNIEnv of the thread.");

            auto input = env->GetFloatArrayElements(inputArray, nullptr);
            if(!input)
                throw std::runtime_error("failed to lock input image for reading.");

            int imageBufferSize = 4 * sizeof(float) * width * height;

            auto imageBuffer = std::make_shared<Buffer>(device, Buffer::ReadWrite,
                    imageBufferSize);
            imageBuffer->copyFrom(input, imageBufferSize); //TODO: Calcular Image Buffer Size

            env->ReleaseFloatArrayElements(inputArray, input, 0);
            env->DeleteGlobalRef(inputArray);

            //Buffer output sum

            auto input_buffer_sum = env->GetFloatArrayElements(output_sum, nullptr);
            if(!input_buffer_sum)
                throw std::runtime_error("failed to lock input image for reading.");

            auto out_sum_buffer = std::make_shared<Buffer>(device, Buffer::ReadWrite,
                    sizeof(float));
            out_sum_buffer->copyFrom(input_buffer_sum, sizeof(float));

            kernelHash["user_log_average"]
                ->setArg(0, imageBuffer)
                ->setArg(1, out_sum_buffer)
                ->setArg(2, base_sum)
                ->setArg(3, height)
                ->setArg(4, width)
                ->setWorkRange(0,1);
        });

        task->setFinishFunction([=] (Device &device, Task::KernelHash &kernelHash) {

            printError("DEBUG: Finish Function called");

            JNIEnv *env;
            int status = _jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
            if(status != JNI_OK)
                throw std::runtime_error("failed to get the JNIEnv of the thread.");

            printError("DEBUG: Finish Function initialized");

            printError("DEBUG: Starting to copy output");

            jboolean isCopy;
            jfloat* jBytes = env->GetFloatArrayElements(output_sum, &isCopy);

            kernelHash["user_log_average"]->buffer(1)->copyTo(jBytes, sizeof(float));

            env->ReleaseFloatArrayElements(output_sum, jBytes, 0);

            env->DeleteGlobalRef(output_sum);

            printError("DEBUG: Output should be written");

        });

        _runtime->submitTask(std::move(task));

        waitFinish();

}

void ParallelMERuntime::run(JNIEnv* mainEnv, jint output_arg_id){

    printError("DEBUG: Run Task called");

    //Find TaskID
    int taskID;
    std::map<jint,std::pair<std::string,jint>>::iterator ref;
    ref = outputPerKernel.find(output_arg_id);
    if(ref != outputPerKernel.end()){
        taskID = ref->second.second;
    }

    //Creating and adding kernels into a task
    auto task = std::make_unique<Task>(_program);
    if(calls.find(taskID) != calls.end()){
        for (std::list<KernelCall>::iterator it  = calls.find(taskID)->second.begin();
             it != calls.find(taskID)->second.end(); ++it){
             printError("DEBUG: Adding kernel %s to task #%d", it->kernel_name.c_str(), taskID);
            task->addKernel(it->kernel_name.c_str());
        }
    }
    else{
        printError("ERRO FATAL!\n");
    }

    printError("DEBUG: Task have all %d kernels", calls.size());

    //[=] means that any external variable is implicitly captured by value if used
    task->setConfigFunction([=] (Device &device, Task::KernelHash &kernelHash) {
        printError("DEBUG: Config function called");

        JNIEnv *env;
        int status = _jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
        if(status != JNI_OK)
            throw std::runtime_error("failed to get the JNIEnv of the thread.");

        int arg_index;
        for (std::list<KernelCall>::iterator it=calls.find(taskID)->second.begin(); it != calls.find(taskID)->second.end();
             ++it){
            Kernel* k = kernelHash[it->kernel_name];
            arg_index = 0;
            //Setting Input Arguments
            printError("DEBUG: Setting Input Arguments");
            for (std::list<int>::iterator inArg=it->input_args.begin(); inArg !=
                                                                        it->input_args.end(); ++inArg){

                std::map<jint,std::shared_ptr<Buffer>>::iterator bufRef;
                bufRef = bufferTable.find(*inArg);
                if(bufRef != bufferTable.end()){
                    k->setArg(arg_index,bufRef->second);
                    arg_index++;
                }
                else{
                    //Creating Allocation Buffer
                    std::shared_ptr<Buffer> allocationBuffer = createNewInputBuffer(env,device,*inArg);
                    //Set argument
                    k->setArg(arg_index,allocationBuffer);
                    arg_index++;
                }
            }

            //Setting Output Arguments
            printError("DEBUG: Setting Output Arguments");
            for (std::list<int>::iterator outArg=it->output_args.begin(); outArg !=
                                                                          it->output_args.end(); ++outArg){

                std::map<jint,std::shared_ptr<Buffer>>::iterator bufRef;
                bufRef = bufferTable.find(*outArg);
                if(bufRef != bufferTable.end()){
                    k->setArg(arg_index,bufRef->second);
                    arg_index++;
                }
                else{
                    //Creating Allocation Buffer
                    printError("DEBUG: Creating allocation buffer");
                    std::shared_ptr<Buffer> allocationBuffer = createNewOutputBuffer(env,device,*outArg);
                    //Set argument
                    printError("DEBUG: Setting buffer as argument");
                    k->setArg(arg_index,allocationBuffer);
                    arg_index++;
                }
            }

            printError("DEBUG: Setting Extra Arguments");
            for (std::list<ExtraArgument>::iterator arg=it->extra_args.begin(); arg
                                                                                != it->extra_args.end(); ++arg){
                printError("DEBUG: Setting Extra Argument #%d", arg_index);
                k->setArg(arg_index, *arg);
                arg_index++;
            }

            printError("DEBUG: Setting worksize");

            k->setWorkRange(0, it->workSize);
        }

        printError("DEBUG: Kernel Calls lined");

    });

    printError("DEBUG: Setting finish function");

    task->setFinishFunction([=] (Device &device, Task::KernelHash &kernelHash) {

        printError("DEBUG: Finish Function called");

        JNIEnv *env;
        int status = _jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
        if(status != JNI_OK)
            throw std::runtime_error("failed to get the JNIEnv of the thread.");


        printError("DEBUG: Finish Function initialized");

        for (std::list<KernelCall>::iterator it=calls.find(taskID)->second.begin(); it != calls.find(taskID)->second.end(); ++it){
            printError("DEBUG: Copying output to Kernel %s", it->kernel_name.c_str());
            for (std::list<int>::iterator outArg=it->output_args.begin(); outArg !=
                                                                          it->output_args.end(); ++outArg){
                jboolean isCopy;
                int outArg_index = it->input_args.size();
                int allocationBufferSize = allocations[*outArg].getSize();
                switch(allocations[*outArg].getType()){
                    case ALLOCATION_TYPE_BYTE: {
                        if(!allocations[*outArg].isDeleted()) {
                            //jbyte* jBytes = env->GetByteArrayElements(reinterpret_cast<jbyteArray>(allocations[*outArg].getPointer()), &isCopy);
                            //kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jBytes,allocationBufferSize);}
                            jbyte* jBytes = env->GetByteArrayElements(allocations[*outArg].getBytePointer(), &isCopy);
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jBytes,allocationBufferSize);
                            env->ReleaseByteArrayElements(allocations[*outArg].getBytePointer(), jBytes, 0);
                            env->DeleteGlobalRef(allocations[*outArg].getBytePointer());
                            allocations[*outArg].deleteReference();
                        }}
                        break;
                    case ALLOCATION_TYPE_BOOLEAN:{
                        if(!allocations[*outArg].isDeleted()) {
                            //jboolean* jBooleans = env->GetBooleanArrayElements(reinterpret_cast<jbooleanArray>(allocations[*outArg].getPointer()), &isCopy);
                            //kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jBooleans,allocationBufferSize);}
                            jboolean* jBooleans = env->GetBooleanArrayElements(allocations[*outArg].getBooleanPointer(), &isCopy);
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jBooleans,allocationBufferSize);
                            env->ReleaseBooleanArrayElements(allocations[*outArg].getBooleanPointer(), jBooleans, 0);
                            env->DeleteGlobalRef(allocations[*outArg].getBooleanPointer());
                            allocations[*outArg].deleteReference();
                        }}
                        break;
                    case ALLOCATION_TYPE_CHAR:{
                        if(!allocations[*outArg].isDeleted()) {
                            //jchar* jChars = env->GetCharArrayElements(reinterpret_cast<jcharArray>(allocations[*outArg].getPointer()), &isCopy);
                            //kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jChars,allocationBufferSize);}
                            jchar* jChars = env->GetCharArrayElements(allocations[*outArg].getCharPointer(), &isCopy);
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jChars,allocationBufferSize);
                            env->ReleaseCharArrayElements(allocations[*outArg].getCharPointer(), jChars, 0);
                            env->DeleteGlobalRef(allocations[*outArg].getCharPointer());
                            allocations[*outArg].deleteReference();
                        }}
                        break;
                    case ALLOCATION_TYPE_SHORT:{
                        if(!allocations[*outArg].isDeleted()) {
                            //jshort* jShorts = env->GetShortArrayElements(reinterpret_cast<jshortArray>(allocations[*outArg].getPointer()), &isCopy);
                            //kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jShorts,allocationBufferSize);}
                            jshort* jShorts = env->GetShortArrayElements(allocations[*outArg].getShortPointer(), &isCopy);
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jShorts,allocationBufferSize);
                            env->ReleaseShortArrayElements(allocations[*outArg].getShortPointer(), jShorts, 0);
                            env->DeleteGlobalRef(allocations[*outArg].getShortPointer());
                            allocations[*outArg].deleteReference();
                        }}
                        break;
                    case ALLOCATION_TYPE_INT:{
                        if(!allocations[*outArg].isDeleted()) {
                            //jint* jInts = env->GetIntArrayElements(reinterpret_cast<jintArray>(allocations[*outArg].getPointer()), &isCopy);
                            //kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jInts,allocationBufferSize);}
                            jint* jInts = env->GetIntArrayElements(allocations[*outArg].getIntPointer(), &isCopy);
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jInts,allocationBufferSize);
                            env->ReleaseIntArrayElements(allocations[*outArg].getIntPointer(), jInts, 0);
                            env->DeleteGlobalRef(allocations[*outArg].getIntPointer());
                            allocations[*outArg].deleteReference();
                         }}
                        break;
                    case ALLOCATION_TYPE_LONG:{
                        if(!allocations[*outArg].isDeleted()) {
                            //jlong* jLongs = env->GetLongArrayElements(reinterpret_cast<jlongArray>(allocations[*outArg].getPointer()), &isCopy);
                            //kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jLongs,allocationBufferSize);}
                            jlong* jLongs = env->GetLongArrayElements(allocations[*outArg].getLongPointer(), &isCopy);
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jLongs,allocationBufferSize);
                            env->ReleaseLongArrayElements(allocations[*outArg].getLongPointer(), jLongs, 0);
                            env->DeleteGlobalRef(allocations[*outArg].getLongPointer());
                            allocations[*outArg].deleteReference();
                        }}
                        break;
                    case ALLOCATION_TYPE_FLOAT:{
                        if(!allocations[*outArg].isDeleted()) {
                            printError("DEBUG: OutArg is #%d index #%d kernel %s size %d", *outArg, outArg_index, outputPerKernel.find(*outArg)->second.first.c_str(), allocationBufferSize);
                            //if(env->IsSameObject(sumPointerReference, allocations[*outArg].getPointer())) printError("DEBUG: Same Reference!");
                            //jfloat* jFloats = env->GetFloatArrayElements(reinterpret_cast<jfloatArray>(allocations[*outArg].getPointer()), &isCopy);
                            jfloat* jFloats = env->GetFloatArrayElements(allocations[*outArg].getFloatPointer(), &isCopy);
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jFloats,allocationBufferSize);
                            env->ReleaseFloatArrayElements(allocations[*outArg].getFloatPointer(),jFloats, 0);
                            env->DeleteGlobalRef(allocations[*outArg].getFloatPointer());
                            allocations[*outArg].deleteReference();
                        }}
                        break;
                    case ALLOCATION_TYPE_DOUBLE:{
                        if(!allocations[*outArg].isDeleted()) {
                            //jdouble* jDoubles = env->GetDoubleArrayElements(reinterpret_cast<jdoubleArray>(allocations[*outArg].getPointer()), &isCopy);
                            //kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jDoubles,allocationBufferSize);}
                            jdouble* jDoubles = env->GetDoubleArrayElements(allocations[*outArg].getDoublePointer(), &isCopy);
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jDoubles,allocationBufferSize);
                            env->ReleaseDoubleArrayElements(allocations[*outArg].getDoublePointer(), jDoubles, 0);
                            env->DeleteGlobalRef(allocations[*outArg].getDoublePointer());
                            allocations[*outArg].deleteReference();
                        }}
                        break;
                    case ALLOCATION_TYPE_BITMAP:{
                        if(!allocations[*outArg].isDeleted()) {
                            //jdouble* jDoubles = env->GetDoubleArrayElements(reinterpret_cast<jdoubleArray>(allocations[*outArg].getPointer()), &isCopy);
                            //kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(jDoubles,allocationBufferSize);}
                            void *output;
                            int ret = AndroidBitmap_lockPixels(env, allocations[*outArg].getBitmapPointer(), &output);
                            if(ret < 0)
                                throw std::runtime_error("failed to lock bitmap for writing.");
                            kernelHash[outputPerKernel.find(*outArg)->second.first]->buffer(outArg_index)->copyTo(output,allocationBufferSize);
                            AndroidBitmap_unlockPixels(env, allocations[*outArg].getBitmapPointer());
                            output = nullptr;
                            env->DeleteGlobalRef(allocations[*outArg].getBitmapPointer());
                            allocations[*outArg].deleteReference();
                        }}
                        break;
                }
                outArg_index++;
            }
        }
        printError("DEBUG: Output should be written");

        printError("DEBUG: Clearing tasks");

        if(calls.find(taskID) != calls.end()){
            printError("DEBUG: Clearing task %d with %d kernels", taskID, calls.find(taskID)->second.size());
            calls.find(taskID)->second.clear();
            printError("DEBUG: Task %d clear", taskID);

        }

    });

    printError("DEBUG: Submitting tasks");

    _runtime->submitTask(std::move(task));


}

/*
 * TODO: COMO FAZER A RUN GENERICA??? PROBLEMA!
 */
 void ParallelMERuntime::run(JNIEnv* mainEnv, jbyteArray input_array, jint input_array_id, size_t imageBufferSize, jfloatArray
         output_array, size_t dataBufferSize){
         //TODO: IMPLEMENTAR
         }

void ParallelMERuntime::run(JNIEnv* mainEnv, jbyteArray input_array, jint input_array_id, size_t imageBufferSize, jobject
bitmap, size_t dataBufferSize, jint workSize, jfloat power){

    printError("DEBUG: Run Byte-Bitmap called");
    printError("DEBUG: External imageBufferSize %d", imageBufferSize);
    printError("DEBUG: External dataBufferSize %d", dataBufferSize);

    std::map<jint, std::list<KernelCall>> copy(calls); //WEIRD!!!

    // Get references to the imageDataArray and bitmap.
    auto inputArray = reinterpret_cast<jbyteArray>(mainEnv->NewGlobalRef(input_array));

    printError("DEBUG: After inputArray created");

    auto outputBitmap = mainEnv->NewGlobalRef(bitmap);
    printError("DEBUG: After outputBitmap created");

    //Creating and adding kernels into a task
    auto task = std::make_unique<Task>(_program);

    printError("DEBUG: Task Input Array ID %d", input_array_id);

    if(calls.find(input_array_id) != calls.end()){
        for (std::list<KernelCall>::iterator it  = calls.find(input_array_id)->second.begin();
             it != calls.find(input_array_id)->second.end(); ++it){
            printError("DEBUG: Adding kernel %s", it->kernel_name.c_str());
            task->addKernel(it->kernel_name.c_str());
        }
    }
    else{
        printError("ERRO FATAL!\n");
    }

    task->addKernel("to_bitmap");

    //[=] means that any external variable is implicitly captured by value if used
    task->setConfigFunction([=] (Device &device, Task::KernelHash &kernelHash) {
        printError("DEBUG: Config function called");

        JNIEnv *env;
        int status = _jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
        if(status != JNI_OK)
            throw std::runtime_error("failed to get the JNIEnv of the thread.");

        printError("DEBUG: Creating input from inputArray");

        auto input = env->GetByteArrayElements(inputArray, nullptr);
        if(!input)
            throw std::runtime_error("failed to lock input image for reading.");

        printError("DEBUG: Creating imageBuffer");

        printError("DEBUG: Internal imageBufferSize %d", imageBufferSize);
        printError("DEBUG: Internal dataBufferSize %d", dataBufferSize);

        auto imageBuffer = std::make_shared<Buffer>(device, Buffer::ReadWrite,
                                                    imageBufferSize);

        printError("DEBUG: Copying input to imageBuffer");

        imageBuffer->copyFrom(input, imageBufferSize);

        env->ReleaseByteArrayElements(inputArray, input, 0);
        env->DeleteGlobalRef(inputArray);

        printError("DEBUG: Creating databuffer");

        auto dataBuffer = std::make_shared<Buffer>(device, Buffer::ReadWrite,
                                                   dataBufferSize);

        printError("DEBUG: Config Function Initialized");

        int arg_index;
        bool first_kernel(true);

        printError("DEBUG: Task have %d kernels for Input Array ID %d", copy.find(input_array_id)->second.size(), input_array_id);

        for (std::list<KernelCall>::const_iterator it=copy.find(input_array_id)->second.begin(); it != copy.find(input_array_id)->second.end();
             ++it){
            Kernel* k = kernelHash[it->kernel_name];

            printError("DEBUG: Kernel Name %s", it->kernel_name.c_str());

            if(first_kernel) {
                k->setArg(0, imageBuffer);
                k->setArg(1, dataBuffer);
                arg_index = 2;
                first_kernel = false;
            }
            else{
                k->setArg(0, dataBuffer);
                arg_index = 1;
            }

            printError("DEBUG: Extra Arguments size: %d", it->extra_args.size());
            for (std::list<ExtraArgument>::const_iterator arg=it->extra_args.begin(); arg != it->extra_args.end(); ++arg){
                printError("DEBUG: Setting Extra Argument %d", arg_index);
                k->setArg(arg_index, *arg);
                printError("DEBUG: Seted Extra Argument %d", arg_index);
                arg_index++;
            }

            printError("DEBUG: Setting worksize");
            printError("DEBUG: worksize %d", it->workSize);
            k->setWorkRange(0, it->workSize);
        }

        kernelHash["to_bitmap"]
                ->setArg(0, dataBuffer)
                ->setArg(1, imageBuffer)
                ->setArg(2, power)
                ->setWorkRange(3, workSize);

        printError("DEBUG: Kernel Calls lined");

    }); //Set config function end

    printError("DEBUG: Setting finish function");

    task->setFinishFunction([=] (Device &device, Task::KernelHash &kernelHash) {

        printError("DEBUG: Finish Function called");

        JNIEnv *env;
        int status = _jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
        if(status != JNI_OK)
            throw std::runtime_error("failed to get the JNIEnv of the thread.");

        void *output;
        int ret = AndroidBitmap_lockPixels(env, outputBitmap, &output);
        if(ret < 0)
            throw std::runtime_error("failed to lock bitmap for writing.");

        kernelHash["to_bitmap"]->buffer(1)->copyTo(output, imageBufferSize);

        output = nullptr;
        AndroidBitmap_unlockPixels(env, outputBitmap);
        env->DeleteGlobalRef(outputBitmap);

    });

    printError("DEBUG: Submitting tasks");

    _runtime->submitTask(std::move(task));

}

void ParallelMERuntime::waitFinish() {
    _runtime->finish();
}