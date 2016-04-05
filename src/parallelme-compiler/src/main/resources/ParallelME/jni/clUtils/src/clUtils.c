/*
 * Author: Renato Utsch Gonçalves
 * Computer Science, UFMG
 * Advanced Computer Graphics
 * Practical exercise 1 - Distributed Ray Tracer
 *
 * Copyright (c) 2015 Renato Utsch <renatoutsch@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

#include <clUtils.h>
#include <stdio.h>
#include <stdlib.h>

#ifdef __ANDROID__ // use __android_log_print
#   include <android/log.h>
#   define CLU_ERROR_PRINTER_FUNC(...) __android_log_print(ANDROID_LOG_ERROR, "clUtils", __VA_ARGS__)
#else // use fprintf
#   define CLU_ERROR_PRINTER_FUNC(...) fprintf(stderr, __VA_ARGS__)
#endif

#define clPrintError(...) do { CLU_ERROR_PRINTER_FUNC(__VA_ARGS__); } while(0)

int cluLoadSource(const char *filename, size_t bufferSize, char *buffer,
        long *fileSize) {
    FILE *fp = fopen(filename, "rb");
    if(!fp) return -1;

    if(buffer) {
        if(fread(buffer, sizeof(*buffer), bufferSize - 1, fp) != bufferSize - 1)
            return -2;
        buffer[bufferSize - 1] = '\0';
    }

    if(fileSize) {
        if(fseek(fp, 0, SEEK_END))
            return -3;

        *fileSize = ftell(fp);
        if(*fileSize == -1)
            return -4;

        *fileSize += 1; // Count the '\0' in.
    }

    fclose(fp);
    return 0;
}

cl_program cluBuildProgram(cl_context context, cl_device_id device,
        const char *source, size_t sourceSize, const char *options, int *err) {
    cl_program program = clCreateProgramWithSource(context, 1,
            (const char **) &source, &sourceSize, err);
    if(*err < 0)
        return NULL;

    *err = clBuildProgram(program, 0, NULL, options, NULL, NULL);
    if(*err < 0) {
        size_t logSize;

        *err = clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, 0,
                NULL, &logSize);
        if(*err < 0) // Nothing we can do.
            return NULL;

        char *programLog = (char *) malloc(logSize + 1);
        if(!programLog) return NULL;
        programLog[logSize] = '\0';

        *err = clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG,
                logSize + 1, programLog, NULL);
        if(*err < 0) { // Nothing we can do [2].
            free(programLog);
            return NULL;
        }

        clPrintError("OpenCL Kernel compilation failed:\n%s", programLog);

        free(programLog);
        *err = -1;
        return NULL;
    }

    return program;
}
