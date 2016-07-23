/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.ImageTranslatorTest;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.Float32;
import org.parallelme.compiler.userlibrary.classes.Pixel;
import org.stringtemplate.v4.ST;

/**
 * Base class for ParallelME run-time image tests.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMImageTranslatorTest extends ImageTranslatorTest {
	@Override
	protected String getParameterType() {
		return Pixel.getInstance().getClassName();
	}

	@Override
	protected String getMapType() {
		return Float32.getInstance().getClassName();
	}

	@Override
	protected String getTranslatedParameterType() {
		return "float3";
	}

	@Override
	protected String getTranslatedMapType() {
		return "float";
	}

	/**
	 * Tests object declaration.
	 */
	@Test
	public void translateObjDeclaration() throws Exception {
		BaseUserLibraryTranslator translator = this.getTranslator();
		InputBind inputBind = createInputBind();
		assertEquals(translator.translateObjDeclaration(inputBind),
				String.format("private long  PM_%s%sPtr;", inputBind.variable,
						inputBind.variable.sequentialNumber));
		Operation operation = createMapOperation(ExecutionType.Sequential);
		assertEquals(translator.translateObjDeclaration(operation),
				String.format("private long  PM_%s%sPtr;",
						operation.destinationVariable,
						operation.destinationVariable.sequentialNumber));
		operation = createMapOperation(ExecutionType.Parallel);
		assertEquals(translator.translateObjDeclaration(operation),
				String.format("private long  PM_%s%sPtr;",
						operation.destinationVariable,
						operation.destinationVariable.sequentialNumber));
	}

	/**
	 * Tests foreach operation translation.
	 */
	@Test
	public void translateForeachOperation() throws Exception {
		// Parallel
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		String expectedTranslation = "__kernel void foreach123(__global float4* PM_data) {"
				+ "int PM_gid = get_global_id(0);"
				+ "float4 param1 = PM_data[PM_gid];"
				+ "param1.s0 = 123; "
				+ "PM_data[PM_gid] = param1;" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String.format(
				"__kernel void foreach123(__global float4* PM_data, %s %s) {"
						+ "int PM_gid = get_global_id(0);"
						+ "float4 param1 = PM_data[PM_gid];"
						+ "param1.s0 = 123; " + "PM_data[PM_gid] = param1;"
						+ "}", finalVar.typeName, finalVar.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("__kernel void foreach123(__global float4 *PM_data, int PM_width, int PM_height, %s %s, __global %s* %s) {"
						+ "for (int PM_y=0; PM_y<PM_height; ++PM_y) {"
						+ "for(int PM_x=0; PM_x<PM_width; ++PM_x) {"
						+ "float4 param1 = PM_data[PM_y*PM_width+PM_x];"
						+ "param1.s0 = 123;"
						+ "PM_data[PM_y*PM_width+PM_x] = param1;"
						+ "}}*%s=%s;}", nonFinalVar.typeName, nonFinalVar.name,
						nonFinalVar.typeName,
						this.commonDefinitions.getPrefix() + nonFinalVar.name,
						this.commonDefinitions.getPrefix() + nonFinalVar.name,
						nonFinalVar.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "__kernel void foreach123(__global float4 *PM_data, int PM_width, int PM_height) {"
				+ "for (int PM_y=0; PM_y<PM_height; ++PM_y) {"
				+ "for(int PM_x=0; PM_x<PM_width; ++PM_x) {"
				+ "float4 param1 = PM_data[PM_y*PM_width+PM_x];"
				+ "param1.s0 = 123;"
				+ "PM_data[PM_y*PM_width+PM_x] = param1;"
				+ "}}}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("__kernel void foreach123(__global float4 *PM_data, int PM_width, int PM_height, %s %s) {"
						+ "for (int PM_y=0; PM_y<PM_height; ++PM_y) {"
						+ "for(int PM_x=0; PM_x<PM_width; ++PM_x) {"
						+ "float4 param1 = PM_data[PM_y*PM_width+PM_x];"
						+ "param1.s0 = 123;"
						+ "PM_data[PM_y*PM_width+PM_x] = param1;" + "}}}",
						finalVar.typeName, finalVar.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("__kernel void foreach123(__global float4 *PM_data, int PM_width, int PM_height, %s %s, __global %s* %s) {"
						+ "for (int PM_y=0; PM_y<PM_height; ++PM_y) {"
						+ "for(int PM_x=0; PM_x<PM_width; ++PM_x) {"
						+ "float4 param1 = PM_data[PM_y*PM_width+PM_x];"
						+ "param1.s0 = 123;"
						+ "PM_data[PM_y*PM_width+PM_x] = param1;"
						+ "}}*%s=%s;}", nonFinalVar.typeName, nonFinalVar.name,
						nonFinalVar.typeName,
						this.commonDefinitions.getPrefix() + nonFinalVar.name,
						this.commonDefinitions.getPrefix() + nonFinalVar.name,
						nonFinalVar.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests foreach operation call.
	 */
	@Test
	public void translateForeachOperationCall() throws Exception {
		// Parallel
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		String varName = this.commonDefinitions
				.getPointerName(operation.variable);
		String expectedTranslation = String
				.format("foreach123(ParallelMERuntime.getInstance().runtimePointer, %s);",
						varName);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests foreach operation JNI interface.
	 */
	@Test
	public void translateForeachOperationJNI() throws Exception {
		// Parallel
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		ParallelMERuntimeCTranslation cTranslator = new ParallelMERuntimeCTranslation();
		String translatedFunction = cTranslator.createParallelOperation(
				operation, this.className);
		String expectedTranslation = "JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr) {\n"
				+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
				+ "auto variablePtr = (ImageData *) varPtr;\n"
				+ "auto task = std::make_unique<Task>(runtimePtr->program);\n"
				+ "task->addKernel(\"foreach123\");\n"
				+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
				+ "kernelHash[\"foreach123\"]\n"
				+ "->setArg(0, variablePtr->outputBuffer)\n"
				+ "->setWorkSize(variablePtr->workSize);\n"
				+ "});\n"
				+ "runtimePtr->runtime->submitTask(std::move(task));\n"
				+ "runtimePtr->runtime->finish();\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createParallelOperation(operation,
				this.className);
		ST st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, <finalVarType> <finalVar>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ImageData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program);\n"
						+ "task->addKernel(\"foreach123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, variablePtr->outputBuffer)\n"
						+ "->setArg(1, <finalVar>)\n"
						+ "->setWorkSize(variablePtr->workSize);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n" + "}");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		expectedTranslation = "JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr) {\n"
				+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
				+ "auto variablePtr = (ImageData *) varPtr;\n"
				+ "auto task = std::make_unique<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
				+ "task->addKernel(\"foreach123\");\n"
				+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
				+ "kernelHash[\"foreach123\"]\n"
				+ "->setArg(0, variablePtr->outputBuffer)\n"
				+ "->setArg(1, variablePtr->width)\n"
				+ "->setArg(2, variablePtr->height)\n"
				+ "->setWorkSize(1);\n"
				+ "});\n"
				+ "runtimePtr->runtime->submitTask(std::move(task));\n"
				+ "runtimePtr->runtime->finish();\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, <finalVarType> <finalVar>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ImageData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "task->addKernel(\"foreach123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, variablePtr->outputBuffer)\n"
						+ "->setArg(1, variablePtr->width)\n"
						+ "->setArg(2, variablePtr->height)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n" + "}");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, "
						+ "<nonFinalVarType> <nonFinalVar>, j<nonFinalVarType>Array PM_<nonFinalVar>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ImageData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVar>));\n"
						+ "task->addKernel(\"foreach123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, variablePtr->outputBuffer)\n"
						+ "->setArg(1, variablePtr->width)\n"
						+ "->setArg(2, variablePtr->height)\n"
						+ "->setArg(3, <nonFinalVar>)\n"
						+ "->setArg(4, <nonFinalVar>Buffer)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);"
						+ "}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests reduce operation translation.
	 */
	@Test
	public void translateReduceOperation() throws Exception {
		// Parallel
		Operation operation = this
				.createReduceOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		String expectedTranslation = "float4 reduce123_func(float4 param1, float4 param2) {\n"
				+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
				+ "__kernel void reduce123_tile(__global float4* PM_data, __global float4* PM_tile, int PM_width) {"
				+ "int PM_gid = get_global_id(0);"
				+ "int PM_base = PM_gid*PM_width;"
				+ "float4 param1 = PM_data[PM_base];"
				+ "float4 param2;"
				+ "for (int PM_x=1;PM_x<PM_width;++PM_x) {"
				+ "param2 = PM_data[PM_base+PM_x];"
				+ "param1 = reduce123_func(param1,param2);"
				+ "}"
				+ "PM_tile[PM_gid]=param1;"
				+ "}"
				+ "__kernel void reduce123(__global float4* PM_destVar, __global float4* PM_tile, int PM_height) {"
				+ "float4 param1 = PM_tile[0];"
				+ "float4 param2;"
				+ "for (int PM_x=1; PM_x<PM_height; ++PM_x){"
				+ "param2 = PM_tile[PM_x];"
				+ "param1 = reduce123_func(param1,param2);"
				+ "}"
				+ "*PM_destVar = param1; }";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("float4 reduce123_func(float4 param1, float4 param2, %s %s) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123_tile(__global float4* PM_data, __global float4* PM_tile, int PM_width, %s %s) {"
						+ "int PM_gid = get_global_id(0);"
						+ "int PM_base = PM_gid*PM_width;"
						+ "float4 param1 = PM_data[PM_base];"
						+ "float4 param2;"
						+ "for (int PM_x=1;PM_x<PM_width;++PM_x) {"
						+ "param2 = PM_data[PM_base+PM_x];"
						+ "param1 = reduce123_func(param1,param2, %s);"
						+ "}"
						+ "PM_tile[PM_gid]=param1;"
						+ "}"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4* PM_tile, int PM_height, %s %s) {"
						+ "float4 param1 = PM_tile[0];"
						+ "float4 param2;"
						+ "for (int PM_x=1; PM_x<PM_height; ++PM_x){"
						+ "param2 = PM_tile[PM_x];"
						+ "param1 = reduce123_func(param1,param2,  %s);"
						+ "}"
						+ "*PM_destVar = param1; }", finalVar.typeName,
						finalVar.name, finalVar.typeName, finalVar.name,
						finalVar.name, finalVar.typeName, finalVar.name,
						finalVar.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		String tmpVarName = this.commonDefinitions.getPrefix() + finalVar.name;
		expectedTranslation = String
				.format("float4 reduce123_func(float4 param1, float4 param2, %s %s, __global %s* %s) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4 *PM_data, int PM_width, int PM_height, %s %s, __global %s* %s) {"
						+ "float4 param1 = PM_data[0];"
						+ "float4 param2;"
						+ "int PM_workSize = PM_height*PM_width;"
						+ "for (int PM_x=0; PM_x<PM_workSize; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1,param2, %s, %s);"
						+ "}*%s= param1;}", finalVar.typeName, finalVar.name,
						finalVar.typeName, tmpVarName, finalVar.typeName,
						finalVar.name, finalVar.typeName, tmpVarName,
						finalVar.name, tmpVarName,
						this.commonDefinitions.getPrefix()
								+ operation.destinationVariable.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("float4 reduce123_func(float4 param1, float4 param2) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4 *PM_data, int PM_width, int PM_height) {"
						+ "float4 param1 = PM_data[0];"
						+ "float4 param2;"
						+ "int PM_workSize = PM_height*PM_width;"
						+ "for (int PM_x=0; PM_x<PM_workSize; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1,param2);"
						+ "}*%s= param1;}", this.commonDefinitions.getPrefix()
						+ operation.destinationVariable.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("float4 reduce123_func(float4 param1, float4 param2, %s %s) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4 *PM_data, int PM_width, int PM_height, %s %s) {"
						+ "float4 param1 = PM_data[0];"
						+ "float4 param2;"
						+ "int PM_workSize = PM_height*PM_width;"
						+ "for (int PM_x=0; PM_x<PM_workSize; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1,param2, %s);"
						+ "}*%s= param1;}", finalVar.typeName, finalVar.name,
						finalVar.typeName, finalVar.name, finalVar.name,
						this.commonDefinitions.getPrefix()
								+ operation.destinationVariable.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		tmpVarName = this.commonDefinitions.getPrefix() + nonFinalVar.name;
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("float4 reduce123_func(float4 param1, float4 param2, %s %s, __global %s* %s) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4 *PM_data, int PM_width, int PM_height, %s %s, __global %s* %s) {"
						+ "float4 param1 = PM_data[0];"
						+ "float4 param2;"
						+ "int PM_workSize = PM_height*PM_width;"
						+ "for (int PM_x=0; PM_x<PM_workSize; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1,param2, %s, %s);"
						+ "}*%s= param1;}", nonFinalVar.typeName,
						nonFinalVar.name, nonFinalVar.typeName, tmpVarName,
						nonFinalVar.typeName, nonFinalVar.name,
						nonFinalVar.typeName, tmpVarName, nonFinalVar.name,
						tmpVarName, this.commonDefinitions.getPrefix()
								+ operation.destinationVariable.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests reduce operation call.
	 */
	@Test
	public void translateReduceOperationCall() throws Exception {
		// Parallel
		Operation operation = this
				.createReduceOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		String imageVarName = this.commonDefinitions
				.getPointerName(operation.variable);
		String tmpVar = this.commonDefinitions.getPrefix()
				+ operation.destinationVariable.name;
		String expectedTranslation = String
				.format("float[] %s = new float[4];\n"
						+ "reduce123(ParallelMERuntime.getInstance().runtimePointer, %s, %s);\n"
						+ "return new Pixel(%s[0], %s[1], %s[2], %s[3], -1, -1);",
						tmpVar, imageVarName, tmpVar, tmpVar, tmpVar, tmpVar,
						tmpVar);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests reduce operation JNI interface.
	 */
	@Test
	public void translateReduceOperationJNI() throws Exception {
		// Parallel
		Operation operation = this
				.createReduceOperation(ExecutionType.Parallel);
		ParallelMERuntimeCTranslation cTranslator = new ParallelMERuntimeCTranslation();
		String translatedFunction = cTranslator.createParallelOperation(
				operation, this.className);
		ST st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, jfloatArray <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ImageData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program);\n"
						+ "auto tileElemSize = sizeof(float)*env->GetArrayLength(<destName>);\n"
						+ "auto tileBuffer = std::make_shared\\<Buffer>(tileElemSize*variablePtr->height);"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(tileElemSize);\n"
						+ "task->addKernel(\"reduce123_tile\");\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123_tile\"]\n"
						+ "->setArg(0, variablePtr->outputBuffer)\n"
						+ "->setArg(1, tileBuffer)\n"
						+ "->setArg(2, variablePtr->width)\n"
						+ "->setWorkSize(variablePtr->height);\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, tileBuffer)\n"
						+ "->setArg(2, variablePtr->height)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createParallelOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, <finalVarType> <finalVar>, jfloatArray <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ImageData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program);\n"
						+ "auto tileElemSize = sizeof(float)*env->GetArrayLength(<destName>);\n"
						+ "auto tileBuffer = std::make_shared\\<Buffer>(tileElemSize*variablePtr->height);"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(tileElemSize);\n"
						+ "task->addKernel(\"reduce123_tile\");\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123_tile\"]\n"
						+ "->setArg(0, variablePtr->outputBuffer)\n"
						+ "->setArg(1, tileBuffer)\n"
						+ "->setArg(2, variablePtr->width)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(variablePtr->height);\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, tileBuffer)\n"
						+ "->setArg(2, variablePtr->height)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, jfloatArray <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ImageData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(sizeof(float)*GetArrayLength(env, <destName>));\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, variablePtr->outputBuffer)\n"
						+ "->setArg(2, variablePtr->width)\n"
						+ "->setArg(3, variablePtr->height)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, <finalVarType> <finalVar>, jfloatArray <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ImageData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(sizeof(float)*GetArrayLength(env, <destName>));\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, variablePtr->outputBuffer)\n"
						+ "->setArg(2, variablePtr->width)\n"
						+ "->setArg(3, variablePtr->height)\n"
						+ "->setArg(4, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, "
						+ "<nonFinalVarType> <nonFinalVar>, j<nonFinalVarType>Array PM_<nonFinalVar>, jfloatArray <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ImageData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(sizeof(float)*GetArrayLength(env, <destName>));\n"
						+ "auto <nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVar>));\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, variablePtr->outputBuffer)\n"
						+ "->setArg(2, variablePtr->width)\n"
						+ "->setArg(3, variablePtr->height)\n"
						+ "->setArg(4, <nonFinalVar>)\n"
						+ "->setArg(5, <nonFinalVar>Buffer)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests method call translation.
	 */
	@Test
	public void translateMethodCall() throws Exception {
		// getHeight method
		MethodCall methodCall = this.createMethodCall(BitmapImage.getInstance()
				.getHeightMethodName());
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateMethodCall(className,
				methodCall);
		String varName = this.commonDefinitions
				.getPointerName(methodCall.variable);
		String expectedTranslation = String.format(
				"return ParallelMERuntime.getInstance().%s(%s);", BitmapImage
						.getInstance().getHeightMethodName(), varName);
		this.validateTranslation(expectedTranslation, translatedFunction);
		methodCall = this.createMethodCall(BitmapImage.getInstance()
				.getWidthMethodName());
		translatedFunction = translator.translateMethodCall(className,
				methodCall);
		expectedTranslation = String.format(
				"return ParallelMERuntime.getInstance().%s(%s);", BitmapImage
						.getInstance().getWidthMethodName(), varName);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}
}
