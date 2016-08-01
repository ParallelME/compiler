/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

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
		return "float4";
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
		ST st = new ST("private long <varName>;\nprivate boolean <fromImageVar> = false;");
		st.add("varName", commonDefinitions.getPointerName(inputBind.variable));
		st.add("fromImageVar",
				commonDefinitions.getFromImageBooleanName(inputBind.variable));
		validateTranslation(st.render(),
				translator.translateObjDeclaration(inputBind));
		st.add("fromImageVar",
				commonDefinitions.getFromImageBooleanName(inputBind.variable));
		// Parallel operation
		Operation operation = createMapOperation(ExecutionType.Sequential);
		st = new ST("private long <varName>;\n"
				+ "private boolean <varFromImage> = false;");
		st.add("varName",
				commonDefinitions.getPointerName(operation.destinationVariable));
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		validateTranslation(st.render(),
				translator.translateObjDeclaration(operation));
		// Sequential operation
		operation = createMapOperation(ExecutionType.Parallel);
		st.remove("varName");
		st.remove("varFromImage");
		st.add("varName",
				commonDefinitions.getPointerName(operation.destinationVariable));
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		validateTranslation(st.render(),
				translator.translateObjDeclaration(operation));
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
		ST st = new ST(
				"static <type> foreach123_func(<type> param1, int x, int y) {\n"
						+ "\tparam1.s0 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type>* PM_data, int PM_width) {\n"
						+ "\tint PM_x = get_global_id(0);\n"
						+ "\tint PM_y = get_global_id(1);\n"
						+ "\tint PM_gid = PM_y*PM_width + PM_x;\n"
						+ "\tPM_data[PM_gid] = foreach123_func(PM_data[PM_gid], PM_x, PM_y);\n"
						+ "}");
		st.add("type", getTranslatedParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> foreach123_func(<type> param1, int x, int y, <finalVarType> <finalVar>) {\n"
						+ "\tparam1.s0 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type>* PM_data, int PM_width, <finalVarType> <finalVar>) {\n"
						+ "\tint PM_x = get_global_id(0);\n"
						+ "\tint PM_y = get_global_id(1);\n"
						+ "\tint PM_gid = PM_y*PM_width + PM_x;\n"
						+ "\tPM_data[PM_gid] = foreach123_func(PM_data[PM_gid], PM_x, PM_y, <finalVar>);\n"
						+ "}");
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> foreach123_func(<type> param1, int x, int y, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "\tparam1.s0 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type> *PM_data, int PM_width, int PM_height, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_width; ++PM_x) {"
						+ "for(int PM_y=0; PM_y\\<PM_height; ++PM_y) {"
						+ "\tint PM_gid = PM_x+PM_y*PM_width;\n"
						+ "\tPM_data[PM_gid] = foreach123_func(PM_data[PM_gid], PM_x, PM_y, PM_<nonFinalVar>);\n"
						+ "}}}");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential non-final and final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> foreach123_func(<type> param1, int x, int y, __global <nonFinalVarType>* PM_<nonFinalVar>, <finalVarType> <finalVar>) {\n"
						+ "\tparam1.s0 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type> *PM_data, int PM_width, int PM_height, __global <nonFinalVarType>* PM_<nonFinalVar>, <finalVarType> <finalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_width; ++PM_x) {"
						+ "for(int PM_y=0; PM_y\\<PM_height; ++PM_y) {"
						+ "\tint PM_gid = PM_x+PM_y*PM_width;\n"
						+ "\tPM_data[PM_gid] = foreach123_func(PM_data[PM_gid], PM_x, PM_y, PM_<nonFinalVar>, <finalVar>);\n"
						+ "}}}");
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> foreach123_func(<type> param1, int x, int y, <finalVarType> <finalVar>) {\n"
						+ "\tparam1.s0 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type> *PM_data, int PM_width, int PM_height, <finalVarType> <finalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_width; ++PM_x) {"
						+ "for(int PM_y=0; PM_y\\<PM_height; ++PM_y) {"
						+ "\tint PM_gid = PM_x+PM_y*PM_width;\n"
						+ "\tPM_data[PM_gid] = foreach123_func(PM_data[PM_gid], PM_x, PM_y, <finalVar>);\n"
						+ "}}}");
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> foreach123_func(<type> param1, int x, int y, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "\tparam1.s0 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type> *PM_data, int PM_width, int PM_height, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_width; ++PM_x) {"
						+ "for(int PM_y=0; PM_y\\<PM_height; ++PM_y) {"
						+ "\tint PM_gid = PM_x+PM_y*PM_width;\n"
						+ "\tPM_data[PM_gid] = foreach123_func(PM_data[PM_gid], PM_x, PM_y, PM_<nonFinalVar>);\n"
						+ "}}}");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
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
		ST st = new ST(
				"foreach123(ParallelMERuntime.getInstance().runtimePointer, <varName>);\n"
						+ "<varFromImage> = true;");
		st.add("varFromImage",
				commonDefinitions.getFromImageBooleanName(operation.variable));
		st.add("varName", commonDefinitions.getPointerName(operation.variable));
		validateTranslation(st.render(), translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		validateTranslation(st.render(), translatedFunction);
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
		String expectedTranslation = "JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data) {\n"
				+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
				+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
				+ "auto PM_task = std::make_unique<Task>(PM_runtimePtr->program);\n"
				+ "PM_task->addKernel(\"foreach123\");\n"
				+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
				+ "kernelHash[\"foreach123\"]\n"
				+ "->setArg(0, PM_dataPtr->outputBuffer)\n"
				+ "->setArg(1, PM_dataPtr->width)\n"
				+ "->setWorkSize(PM_dataPtr->width, PM_dataPtr->height);\n"
				+ "});\n"
				+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
				+ "PM_runtimePtr->runtime->finish();\n" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createParallelOperation(operation,
				this.className);
		ST st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, <finalVarType> <finalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program);\n"
						+ "PM_task->addKernel(\"foreach123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(1, PM_dataPtr->width)\n"
						+ "->setArg(2, <finalVar>)\n"
						+ "->setWorkSize(PM_dataPtr->width, PM_dataPtr->height);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n" + "}");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, "
						+ "j<nonFinalVarType>Array PM_<nonFinalVar>, <finalVarType> <finalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto PM_<nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "PM_<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "PM_task->addKernel(\"foreach123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(1, PM_dataPtr->width)\n"
						+ "->setArg(2, PM_dataPtr->height)\n"
						+ "->setArg(3, PM_<nonFinalVar>Buffer)\n"
						+ "->setArg(4, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);"
						+ "}");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, <finalVarType> <finalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "PM_task->addKernel(\"foreach123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(1, PM_dataPtr->width)\n"
						+ "->setArg(2, PM_dataPtr->height)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n" + "}");
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, "
						+ "j<nonFinalVarType>Array PM_<nonFinalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto PM_<nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "PM_<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "PM_task->addKernel(\"foreach123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(1, PM_dataPtr->width)\n"
						+ "->setArg(2, PM_dataPtr->height)\n"
						+ "->setArg(3, PM_<nonFinalVar>Buffer)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);"
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
		String expectedTranslation = "static float4 reduce123_func(float4 param1, float4 param2) {\n"
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
				.format("static float4 reduce123_func(float4 param1, float4 param2, %s %s) {\n"
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
		ST st = new ST(
				"static float4 reduce123_func(float4 param1, float4 param2, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4 *PM_data, int PM_width, int PM_height, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "float4 param1 = PM_data[0];"
						+ "float4 param2;"
						+ "int PM_workSize = PM_height*PM_width;"
						+ "for (int PM_x=0; PM_x\\<PM_workSize; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1,param2, PM_<nonFinalVar>);"
						+ "}*PM_<destVar>= param1;}");
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("destVar", operation.destinationVariable.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static float4 reduce123_func(float4 param1, float4 param2) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4 *PM_data, int PM_width, int PM_height) {"
						+ "float4 param1 = PM_data[0];" + "float4 param2;"
						+ "int PM_workSize = PM_height*PM_width;"
						+ "for (int PM_x=0; PM_x\\<PM_workSize; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1,param2);"
						+ "}*PM_<destVar>= param1;}");
		st.add("destVar", operation.destinationVariable.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static float4 reduce123_func(float4 param1, float4 param2, <finalVarType> <finalVar>) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4 *PM_data, int PM_width, int PM_height, <finalVarType> <finalVar>) {"
						+ "float4 param1 = PM_data[0];"
						+ "float4 param2;"
						+ "int PM_workSize = PM_height*PM_width;"
						+ "for (int PM_x=0; PM_x\\<PM_workSize; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1,param2, <finalVar>);"
						+ "}*PM_<destVar>= param1;}");
		st.add("finalVar", finalVar.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("destVar", operation.destinationVariable.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static float4 reduce123_func(float4 param1, float4 param2, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "param1.s0 = 123; param2.s1=456; return param2;} \n"
						+ "__kernel void reduce123(__global float4* PM_destVar, __global float4 *PM_data, int PM_width, int PM_height, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "float4 param1 = PM_data[0];"
						+ "float4 param2;"
						+ "int PM_workSize = PM_height*PM_width;"
						+ "for (int PM_x=0; PM_x\\<PM_workSize; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1,param2, PM_<nonFinalVar>);"
						+ "}*PM_<destVar>= param1;}");
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("destVar", operation.destinationVariable.name);
		expectedTranslation = st.render();
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
		ST st = new ST(
				"float[] <tmpVar> = new float[4];\n"
						+ "reduce123(ParallelMERuntime.getInstance().runtimePointer, <imageVarName>, <tmpVar>);\n"
						+ "return new Pixel(<tmpVar>[0], <tmpVar>[1], <tmpVar>[2], <tmpVar>[3], -1, -1);");
		st.add("imageVarName", imageVarName);
		st.add("tmpVar", tmpVar);
		this.validateTranslation(st.render(), translatedFunction);
		// Parallel with final variable
		Variable finalVar = createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(
				className, operation);
		st = new ST(
				"float[] <tmpVar> = new float[4];\n"
						+ "reduce123(ParallelMERuntime.getInstance().runtimePointer, <imageVarName>, <tmpVar>, <finalVarName>);\n"
						+ "return new Pixel(<tmpVar>[0], <tmpVar>[1], <tmpVar>[2], <tmpVar>[3], -1, -1);");
		st.add("imageVarName", imageVarName);
		st.add("tmpVar", tmpVar);
		st.add("finalVarName", finalVar.name);
		this.validateTranslation(st.render(), translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		this.validateTranslation(st.render(), translatedFunction);
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
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jfloatArray <destName>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program);\n"
						+ "int PM_tileElemSize = sizeof(float) * env->GetArrayLength(<destName>);\n"
						+ "auto PM_tileBuffer = std::make_shared\\<Buffer>(PM_tileElemSize * PM_dataPtr->height);"
						+ "auto PM_<destName>Buffer = std::make_shared\\<Buffer>(PM_tileElemSize);\n"
						+ "PM_task->addKernel(\"reduce123_tile\");\n"
						+ "PM_task->addKernel(\"reduce123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123_tile\"]\n"
						+ "->setArg(0, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(1, PM_tileBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setWorkSize(PM_dataPtr->height);\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, PM_<destName>Buffer)\n"
						+ "->setArg(1, PM_tileBuffer)\n"
						+ "->setArg(2, PM_dataPtr->height)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<destName>Buffer->copyToJArray(env, <destName>);\n"
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
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jfloatArray <destName>, "
						+ "<finalVarType> <finalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program);\n"
						+ "int PM_tileElemSize = sizeof(float) * env->GetArrayLength(<destName>);\n"
						+ "auto PM_tileBuffer = std::make_shared\\<Buffer>(PM_tileElemSize * PM_dataPtr->height);"
						+ "auto PM_<destName>Buffer = std::make_shared\\<Buffer>(PM_tileElemSize);\n"
						+ "PM_task->addKernel(\"reduce123_tile\");\n"
						+ "PM_task->addKernel(\"reduce123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123_tile\"]\n"
						+ "->setArg(0, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(1, PM_tileBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(PM_dataPtr->height);\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, PM_<destName>Buffer)\n"
						+ "->setArg(1, PM_tileBuffer)\n"
						+ "->setArg(2, PM_dataPtr->height)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<destName>Buffer->copyToJArray(env, <destName>);\n"
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
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jfloatArray <destName>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto PM_<destName>Buffer = std::make_shared\\<Buffer>(sizeof(float)*env->GetArrayLength(<destName>));\n"
						+ "PM_task->addKernel(\"reduce123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, PM_<destName>Buffer)\n"
						+ "->setArg(1, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setArg(3, PM_dataPtr->height)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<destName>Buffer->copyToJArray(env, <destName>);\n"
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
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jfloatArray <destName>, <finalVarType> <finalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto PM_<destName>Buffer = std::make_shared\\<Buffer>(sizeof(float)*env->GetArrayLength(<destName>));\n"
						+ "PM_task->addKernel(\"reduce123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, PM_<destName>Buffer)\n"
						+ "->setArg(1, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setArg(3, PM_dataPtr->height)\n"
						+ "->setArg(4, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<destName>Buffer->copyToJArray(env, <destName>);\n"
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
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, "
						+ "jfloatArray <destName>, j<nonFinalVarType>Array PM_<nonFinalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto PM_<destName>Buffer = std::make_shared\\<Buffer>(sizeof(float)*env->GetArrayLength(<destName>));\n"
						+ "auto PM_<nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "PM_<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "PM_task->addKernel(\"reduce123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, PM_<destName>Buffer)\n"
						+ "->setArg(1, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setArg(3, PM_dataPtr->height)\n"
						+ "->setArg(4, PM_<nonFinalVar>Buffer)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);\n"
						+ "PM_<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}
	

	/**
	 * Tests map operation translation.
	 */
	@Test
	public void translateMapOperation() throws Exception {
		// Parallel
		Operation operation = this.createMapOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		ST st = new ST(
				"static <mapType> map123_func(<type> param1, int x, int y) {\n"
						+ "<mapType> ret;"
						+ "ret = param1.s2 * 1.5f;"
						+ "return ret; } \n"
						+ "__kernel void map123(__global <mapType>* PM_destVar, __global <type>* PM_data, int PM_width) {"
						+ "\tint PM_x = get_global_id(0);\n"
						+ "\tint PM_y = get_global_id(1);\n"
						+ "\tint PM_gid = PM_y * PM_width + PM_x;\n"
						+ "\tPM_destVar[PM_gid] = map123_func(PM_data[PM_gid], PM_x, PM_y);"
						+ "}");
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createMapOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <mapType> map123_func(<type> param1, int x, int y, <finalVarType> <finalVar>) {\n"
						+ "<mapType> ret;"
						+ "ret = param1.s2 * 1.5f;"
						+ "return ret; } \n"
						+ "__kernel void map123(__global <mapType>* PM_destVar, __global <type>* PM_data, int PM_width, <finalVarType> <finalVar>) {"
						+ "\tint PM_x = get_global_id(0);\n"
						+ "\tint PM_y = get_global_id(1);\n"
						+ "\tint PM_gid = PM_y * PM_width + PM_x;\n"
						+ "\tPM_destVar[PM_gid] = map123_func(PM_data[PM_gid], PM_x, PM_y, <finalVar>);"
						+ "}");
		st.add("finalVar", finalVar.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createMapOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <mapType> map123_func(<type> param1, int x, int y, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "<mapType> ret;"
						+ "ret = param1.s2 * 1.5f;"
						+ "return ret; } \n"
						+ "__kernel void map123(__global <mapType>* PM_destVar, __global <type>* PM_data, int PM_width, int PM_height, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_width; ++PM_x) {"
						+ "for(int PM_y=0; PM_y\\<PM_height; ++PM_y) {"
						+ "\tint PM_gid = PM_x + PM_y * PM_width;\n"
						+ "\tPM_destVar[PM_gid] = map123_func(PM_data[PM_gid], PM_x, PM_y, PM_<nonFinalVar>);"
						+ "}}}");
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <mapType> map123_func(<type> param1, int x, int y, __global <nonFinalVarType>* PM_<nonFinalVar>, <finalVarType> <finalVar>) {\n"
						+ "<mapType> ret;"
						+ "ret = param1.s2 * 1.5f;"
						+ "return ret; } \n"
						+ "__kernel void map123(__global <mapType>* PM_destVar, __global <type>* PM_data, int PM_width, int PM_height, "
						+ "__global <nonFinalVarType>* PM_<nonFinalVar>, <finalVarType> <finalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_width; ++PM_x) {"
						+ "for(int PM_y=0; PM_y\\<PM_height; ++PM_y) {"
						+ "\tint PM_gid = PM_x + PM_y * PM_width;\n"
						+ "\tPM_destVar[PM_gid] = map123_func(PM_data[PM_gid], PM_x, PM_y, PM_<nonFinalVar>, <finalVar>);"
						+ "}}}");
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("finalVar", finalVar.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <mapType> map123_func(<type> param1, int x, int y, <finalVarType> <finalVar>) {\n"
						+ "<mapType> ret;"
						+ "ret = param1.s2 * 1.5f;"
						+ "return ret; } \n"
						+ "__kernel void map123(__global <mapType>* PM_destVar, __global <type>* PM_data, int PM_width, int PM_height, "
						+ "<finalVarType> <finalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_width; ++PM_x) {"
						+ "for(int PM_y=0; PM_y\\<PM_height; ++PM_y) {"
						+ "\tint PM_gid = PM_x + PM_y * PM_width;\n"
						+ "\tPM_destVar[PM_gid] = map123_func(PM_data[PM_gid], PM_x, PM_y, <finalVar>);"
						+ "}}}");
		st.add("finalVar", finalVar.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <mapType> map123_func(<type> param1, int x, int y, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "<mapType> ret;"
						+ "ret = param1.s2 * 1.5f;"
						+ "return ret; } \n"
						+ "__kernel void map123(__global <mapType>* PM_destVar, __global <type>* PM_data, int PM_width, int PM_height, "
						+ "__global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_width; ++PM_x) {"
						+ "for(int PM_y=0; PM_y\\<PM_height; ++PM_y) {"
						+ "\tint PM_gid = PM_x + PM_y * PM_width;\n"
						+ "\tPM_destVar[PM_gid] = map123_func(PM_data[PM_gid], PM_x, PM_y, PM_<nonFinalVar>);"
						+ "}}}");
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("type", getTranslatedParameterType());
		st.add("mapType", getTranslatedMapType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests map operation call.
	 */
	@Test
	public void translateMapOperationCall() throws Exception {
		// Parallel
		Operation operation = createMapOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = getTranslator();
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		ST st = new ST(
				"<destPointer> = ParallelMERuntime.getInstance().createArray(<mapClassType>.class, ParallelMERuntime.getInstance().getWidth(<pointerVar>) *"
				+ "ParallelMERuntime.getInstance().getHeight(<pointerVar>));\n"
						+ "map123(ParallelMERuntime.getInstance().runtimePointer, <pointerVar>, <destPointer>);\n"
						+ "<varFromImage> = true;");
		st.add("pointerVar",
				commonDefinitions.getPointerName(operation.variable));
		st.add("destPointer",
				commonDefinitions.getPointerName(operation.destinationVariable));
		st.add("mapClassType", getTranslatedMapType());
		st.add("userLibraryType", getParameterType());
		st.add("varFromImage", commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable));
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createMapOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperationCall(className,
				operation);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests map operation JNI interface.
	 */
	@Test
	public void translateMapOperationJNI() throws Exception {
		// Parallel
		Operation operation = this.createMapOperation(ExecutionType.Parallel);
		ParallelMERuntimeCTranslation cTranslator = new ParallelMERuntimeCTranslation();
		String translatedFunction = cTranslator.createParallelOperation(
				operation, this.className);
		ST st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_map123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jlong PM_dataRet) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_dataRetPtr = (ArrayData *) PM_dataRet;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program);\n"
						+ "PM_task->addKernel(\"map123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"map123\"]\n"
						+ "->setArg(0, PM_dataRetPtr->buffer)\n"
						+ "->setArg(1, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setWorkSize(PM_dataPtr->width, PM_dataPtr->height);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n" + "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedMapType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createMapOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createParallelOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_map123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jlong PM_dataRet,"
						+ "<finalVarType> <finalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_dataRetPtr = (ArrayData *) PM_dataRet;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program);\n"
						+ "PM_task->addKernel(\"map123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"map123\"]\n"
						+ "->setArg(0, PM_dataRetPtr->buffer)\n"
						+ "->setArg(1, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(PM_dataPtr->width, PM_dataPtr->height);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n" + "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedMapType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_map123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jlong PM_dataRet, "
						+ "j<nonFinalVarType>Array PM_<nonFinalVar>, <finalVarType> <finalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_dataRetPtr = (ArrayData *) PM_dataRet;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto PM_<nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "PM_<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "PM_task->addKernel(\"map123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"map123\"]\n"
						+ "->setArg(0, PM_dataRetPtr->buffer)\n"
						+ "->setArg(1, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setArg(3, PM_dataPtr->height)\n"
						+ "->setArg(4, PM_<nonFinalVar>Buffer)\n"
						+ "->setArg(5, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedMapType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_map123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jlong PM_dataRet, "
						+ "<finalVarType> <finalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_dataRetPtr = (ArrayData *) PM_dataRet;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "PM_task->addKernel(\"map123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"map123\"]\n"
						+ "->setArg(0, PM_dataRetPtr->buffer)\n"
						+ "->setArg(1, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setArg(3, PM_dataPtr->height)\n"
						+ "->setArg(4, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n" + "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedMapType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createMapOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_map123(JNIEnv *env, jobject self, jlong PM_runtime, jlong PM_data, jlong PM_dataRet, "
						+ "j<nonFinalVarType>Array PM_<nonFinalVar>) {\n"
						+ "auto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
						+ "auto PM_dataPtr = (ImageData *) PM_data;\n"
						+ "auto PM_dataRetPtr = (ArrayData *) PM_dataRet;\n"
						+ "auto PM_task = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto PM_<nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "PM_<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "PM_task->addKernel(\"map123\");\n"
						+ "PM_task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"map123\"]\n"
						+ "->setArg(0, PM_dataRetPtr->buffer)\n"
						+ "->setArg(1, PM_dataPtr->outputBuffer)\n"
						+ "->setArg(2, PM_dataPtr->width)\n"
						+ "->setArg(3, PM_dataPtr->height)\n"
						+ "->setArg(4, PM_<nonFinalVar>Buffer)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "PM_runtimePtr->runtime->submitTask(std::move(PM_task));\n"
						+ "PM_runtimePtr->runtime->finish();\n"
						+ "PM_<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedMapType());
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
