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
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.ArrayTranslatorTest;
import org.parallelme.compiler.translation.SimpleTranslator;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.stringtemplate.v4.ST;

/**
 * Performs tests to validate PMArrayTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMArrayTranslatorBaseTest extends ArrayTranslatorTest {
	@Override
	protected BaseUserLibraryTranslator getTranslator() {
		return new PMArrayTranslator(new SimpleTranslator());
	}

	/**
	 * Tests input bind translation.
	 */
	@Test
	public void translateInputBind() throws Exception {
		String expectedTranslation = "";
		InputBind inputBind = this.createInputBind();
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateInputBind(className,
				inputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests input bind object creation.
	 */
	@Test
	public void translateInputBindObjCreation() throws Exception {
		InputBind inputBind = this.createInputBind();
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateInputBindObjCreation(
				className, inputBind);
		String varName = this.commonDefinitions
				.getPointerName(inputBind.variable);
		String expectedTranslation = String.format(
				"%s = ParallelMERuntime.getInstance().createArray(%s);",
				varName, inputBind.parameters.get(0));
		this.validateTranslation(expectedTranslation, translatedFunction);
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
	 * Tests output bind translation.
	 */
	@Test
	public void translateOutputBind() throws Exception {
		String expectedTranslation = "";
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBind(className,
				null);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests output bind call.
	 */
	@Test
	public void translateOutputBindCall() throws Exception {
		// arrayVar.toArray(dataVar);
		OutputBind outputBind = this.createOutputBind(OutputBindType.None);
		String varName = this.commonDefinitions
				.getPointerName(outputBind.variable);
		ST st = new ST(
				"ParallelMERuntime.getInstance().toArray(<varName>, <varDest>);");
		st.add("varName", varName);
		st.add("varDest", outputBind.destinationObject);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBindCall(
				className, outputBind);
		this.validateTranslation(st.render(), translatedFunction);
		// bitmapVar = imageVar.toBitmap();
		outputBind = this.createOutputBind(OutputBindType.Assignment);
		st = new ST(
				"<type>[] PM_javaArray = new <type>[ParallelMERuntime.getInstance().getLength(<varDest>)];\n"
						+ "ParallelMERuntime.getInstance().toArray(<varDest>, PM_javaArray);\n"
						+ "return PM_javaArray;");
		st.add("varName", varName);
		st.add("varDest", commonDefinitions.getPointerName(outputBind.variable));
		st.add("type", getTranslatedParameterType());
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		this.validateTranslation(st.render(), translatedFunction);
		// Bitmap bitmapVar = imageVar.toBitmap();
		outputBind = this
				.createOutputBind(OutputBindType.DeclarativeAssignment);
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		this.validateTranslation(st.render(), translatedFunction);
	}

	/**
	 * Tests operation translation.
	 */
	@Test
	public void translateForeachOperation() throws Exception {
		// Parallel
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		BaseUserLibraryTranslator translator = this.getTranslator();
		List<String> translatedFunction = translator
				.translateOperation(operation);
		ST st = new ST("static <type> foreach123_func(<type> param1) {\n"
				+ "\tparam1 = 123;\n\n" + "\treturn param1;\n" + "}\n"
				+ "__kernel void foreach123(__global <type>* PM_data) {\n"
				+ "\tint PM_gid = get_global_id(0);\n"
				+ "\tPM_data[PM_gid] = foreach123_func(PM_data[PM_gid]);\n"
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
				"static <type> foreach123_func(<type> param1, <finalVarType> <finalVar>) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type>* PM_data, <finalVarType> <finalVar>) {\n"
						+ "\tint PM_gid = get_global_id(0);\n"
						+ "\tPM_data[PM_gid] = foreach123_func(PM_data[PM_gid], <finalVar>);\n"
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
				"static <type> foreach123_func(<type> param1, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type> *PM_data, int PM_length, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_length; ++PM_x) {"
						+ "\tPM_data[PM_x] = foreach123_func(PM_data[PM_x], PM_<nonFinalVar>);\n"
						+ "}}");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> foreach123_func(<type> param1) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type> *PM_data, int PM_length) {"
						+ "for(int PM_x=0; PM_x\\<PM_length; ++PM_x) {"
						+ "\tPM_data[PM_x] = foreach123_func(PM_data[PM_x]);\n"
						+ "}}");
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> foreach123_func(<type> param1, <finalVarType> <finalVar>) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type> *PM_data, int PM_length, <finalVarType> <finalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_length; ++PM_x) {"
						+ "\tPM_data[PM_x] = foreach123_func(PM_data[PM_x], <finalVar>);\n"
						+ "}}");
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
				"static <type> foreach123_func(<type> param1, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "\tparam1 = 123;\n\n"
						+ "\treturn param1;\n"
						+ "}\n"
						+ "__kernel void foreach123(__global <type> *PM_data, int PM_length, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_length; ++PM_x) {"
						+ "\tPM_data[PM_x] = foreach123_func(PM_data[PM_x], PM_<nonFinalVar>);\n"
						+ "}}");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests operation call.
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
				+ "auto variablePtr = (ArrayData *) varPtr;\n"
				+ "auto task = std::make_unique<Task>(runtimePtr->program);\n"
				+ "task->addKernel(\"foreach123\");\n"
				+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
				+ "kernelHash[\"foreach123\"]\n"
				+ "->setArg(0, variablePtr->buffer)\n"
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
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program);\n"
						+ "task->addKernel(\"foreach123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, variablePtr->buffer)\n"
						+ "->setArg(1, <finalVar>)\n"
						+ "->setWorkSize(variablePtr->workSize);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n" + "}");
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
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, "
						+ "j<nonFinalVarType>Array PM_<nonFinalVar>, <finalVarType> <finalVar>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "task->addKernel(\"foreach123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, variablePtr->buffer)\n"
						+ "->setArg(1, variablePtr->length)\n"
						+ "->setArg(2, <nonFinalVar>Buffer)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);"
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
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, <finalVarType> <finalVar>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "task->addKernel(\"foreach123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, variablePtr->buffer)\n"
						+ "->setArg(1, variablePtr->length)\n"
						+ "->setArg(2, <finalVar>)\n"
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
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_foreach123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, "
						+ "j<nonFinalVarType>Array PM_<nonFinalVar>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "task->addKernel(\"foreach123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"foreach123\"]\n"
						+ "->setArg(0, variablePtr->buffer)\n"
						+ "->setArg(1, variablePtr->length)\n"
						+ "->setArg(2, <nonFinalVar>Buffer)\n"
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
		ST st = new ST(
				"static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param2 += param1;"
						+ "return param2;} \n"
						+ "__kernel void reduce123_tile(__global <type>* PM_data, __global <type>* PM_tile, int PM_tileSize) {"
						+ "int PM_gid = get_global_id(0);"
						+ "int PM_base = PM_gid*PM_tileSize;"
						+ "<type> param1 = PM_data[PM_base];"
						+ "<type> param2;"
						+ "for (int PM_x=1;PM_x\\<PM_tileSize;++PM_x) {"
						+ "param2 = PM_data[PM_base+PM_x];"
						+ "param1 = reduce123_func(param1,param2);"
						+ "}"
						+ "PM_tile[PM_gid]=param1;"
						+ "}"
						+ "__kernel void reduce123(__global <type>* PM_destVar, __global <type>* PM_data, __global <type>* PM_tile, int PM_length, int PM_tileSize) {"
						+ "<type> param1 = PM_tile[0];"
						+ "<type> param2;"
						+ "for (int PM_x=1; PM_x\\<PM_tileSize; ++PM_x) {"
						+ "param2 = PM_tile[PM_x];"
						+ "param1 = reduce123_func(param1, param2);"
						+ "}"
						+ "for (int PM_x = (int) pow(floor(sqrt((float)PM_length)), 2); PM_x \\< PM_length; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1, param2);" + "}"
						+ "*PM_destVar = param1; }");
		st.add("type", getTranslatedParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> reduce123_func(<type> param1, <type> param2, <finalVarType> <finalVar>) {\n"
						+ "param2 += param1;"
						+ "return param2;} \n"
						+ "__kernel void reduce123_tile(__global <type>* PM_data, __global <type>* PM_tile, int PM_tileSize, <finalVarType> <finalVar>) {"
						+ "int PM_gid = get_global_id(0);"
						+ "int PM_base = PM_gid*PM_tileSize;"
						+ "<type> param1 = PM_data[PM_base];"
						+ "<type> param2;"
						+ "for (int PM_x=1;PM_x\\<PM_tileSize;++PM_x) {"
						+ "param2 = PM_data[PM_base+PM_x];"
						+ "param1 = reduce123_func(param1,param2, <finalVar>);"
						+ "}"
						+ "PM_tile[PM_gid]=param1;"
						+ "}"
						+ "__kernel void reduce123(__global <type>* PM_destVar, __global <type>* PM_data, __global <type>* PM_tile, int PM_length, int PM_tileSize, <finalVarType> <finalVar>) {"
						+ "<type> param1 = PM_tile[0];"
						+ "<type> param2;"
						+ "for (int PM_x=1; PM_x\\<PM_tileSize; ++PM_x) {"
						+ "param2 = PM_tile[PM_x];"
						+ "param1 = reduce123_func(param1, param2, <finalVar>);"
						+ "}"
						+ "for (int PM_x = (int) pow(floor(sqrt((float)PM_length)), 2); PM_x \\< PM_length; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1, param2, <finalVar>);"
						+ "}" + "*PM_destVar = param1; }");
		st.add("finalVar", finalVar.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> reduce123_func(<type> param1, <type> param2, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "param2 += param1;"
						+ "return param2;} \n"
						+ "__kernel void reduce123(__global <type>* PM_destVar, __global <type>* PM_data, int PM_length, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "<type> param1 = PM_data[0];"
						+ "<type> param2;"
						+ "for (int PM_x=1; PM_x\\<PM_length; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1, param2, PM_<nonFinalVar>);"
						+ "}" + "*PM_destVar = param1; }");
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createReduceOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> reduce123_func(<type> param1, <type> param2) {\n"
						+ "param2 += param1;"
						+ "return param2;} \n"
						+ "__kernel void reduce123(__global <type>* PM_destVar, __global <type>* PM_data, int PM_length) {"
						+ "<type> param1 = PM_data[0];" + "<type> param2;"
						+ "for (int PM_x=1; PM_x\\<PM_length; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1, param2);" + "}"
						+ "*PM_destVar = param1; }");
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> reduce123_func(<type> param1, <type> param2, <finalVarType> <finalVar>) {\n"
						+ "param2 += param1;"
						+ "return param2;} \n"
						+ "__kernel void reduce123(__global <type>* PM_destVar, __global <type>* PM_data, int PM_length, <finalVarType> <finalVar>) {"
						+ "<type> param1 = PM_data[0];"
						+ "<type> param2;"
						+ "for (int PM_x=1; PM_x\\<PM_length; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1, param2, <finalVar>);"
						+ "}" + "*PM_destVar = param1; }");
		st.add("finalVar", finalVar.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"static <type> reduce123_func(<type> param1, <type> param2, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "param2 += param1;"
						+ "return param2;} \n"
						+ "__kernel void reduce123(__global <type>* PM_destVar, __global <type>* PM_data, int PM_length, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "<type> param1 = PM_data[0];"
						+ "<type> param2;"
						+ "for (int PM_x=1; PM_x\\<PM_length; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1, param2, PM_<nonFinalVar>);"
						+ "}" + "*PM_destVar = param1; }");
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("type", getTranslatedParameterType());
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
		String tmpVarName = this.commonDefinitions.getPrefix()
				+ operation.destinationVariable.name;
		ST st = new ST(
				"<type>[] <tmpVar> = new <type>[1];\n"
						+ "reduce123(ParallelMERuntime.getInstance().runtimePointer, <imageVar>, <tmpVar>);\n"
						+ "return new <userLibraryType>(<tmpVar>[0]);");
		st.add("type", getTranslatedParameterType());
		st.add("tmpVar", tmpVarName);
		st.add("imageVar", imageVarName);
		st.add("userLibraryType", getParameterType());
		String expectedTranslation = st.render();
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
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, j<type>Array <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program);\n"
						+ "int tileSize = floor(sqrt((float)variablePtr->length));\n"
						+ "auto tileBuffer = std::make_shared\\<Buffer>(sizeof(<type>) * tileSize);"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(sizeof(<type>));\n"
						+ "task->addKernel(\"reduce123_tile\");\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123_tile\"]\n"
						+ "->setArg(0, variablePtr->buffer)\n"
						+ "->setArg(1, tileBuffer)\n"
						+ "->setArg(2, tileSize)\n"
						+ "->setWorkSize(tileSize);\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, variablePtr->buffer)\n"
						+ "->setArg(2, tileBuffer)\n"
						+ "->setArg(3, variablePtr->length)\n"
						+ "->setArg(4, tileSize)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createReduceOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final", "var1");
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createParallelOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, "
						+ "<finalVarType> <finalVar>, j<type>Array <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program);\n"
						+ "int tileSize = floor(sqrt((float)variablePtr->length));\n"
						+ "auto tileBuffer = std::make_shared\\<Buffer>(sizeof(<type>) * tileSize);"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(sizeof(<type>));\n"
						+ "task->addKernel(\"reduce123_tile\");\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123_tile\"]\n"
						+ "->setArg(0, variablePtr->buffer)\n"
						+ "->setArg(1, tileBuffer)\n"
						+ "->setArg(2, tileSize)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(tileSize);\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, variablePtr->buffer)\n"
						+ "->setArg(2, tileBuffer)\n"
						+ "->setArg(3, variablePtr->length)\n"
						+ "->setArg(4, tileSize)\n"
						+ "->setArg(5, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final and final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		Variable nonFinalVar = this.createExternalVariable("", "var2");
		operation.addExternalVariable(nonFinalVar);
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, "
						+ "j<nonFinalVarType>Array PM_<nonFinalVar>, <finalVarType> <finalVar>, j<type>Array <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(sizeof(<type>)*env->GetArrayLength(<destName>));\n"
						+ "auto <nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, variablePtr->buffer)\n"
						+ "->setArg(2, variablePtr->length)\n"
						+ "->setArg(3, <nonFinalVar>Buffer)\n"
						+ "->setArg(4, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, "
						+ "<finalVarType> <finalVar>, j<type>Array <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(sizeof(<type>)*env->GetArrayLength(<destName>));\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, variablePtr->buffer)\n"
						+ "->setArg(2, variablePtr->length)\n"
						+ "->setArg(3, <finalVar>)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createReduceOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = cTranslator.createSequentialOperation(operation,
				this.className);
		st = new ST(
				"JNIEXPORT void JNICALL Java_SomeClass_reduce123(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr, "
						+ "j<nonFinalVarType>Array PM_<nonFinalVar>, j<type>Array <destName>) {\n"
						+ "auto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
						+ "auto variablePtr = (ArrayData *) varPtr;\n"
						+ "auto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f,2.0f));\n"
						+ "auto <destName>Buffer = std::make_shared\\<Buffer>(sizeof(<type>)*env->GetArrayLength(<destName>));\n"
						+ "auto <nonFinalVar>Buffer = std::make_shared\\<Buffer>(sizeof(<nonFinalVarType>));\n"
						+ "<nonFinalVar>Buffer->setJArraySource(env, PM_<nonFinalVar>);\n"
						+ "task->addKernel(\"reduce123\");\n"
						+ "task->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
						+ "kernelHash[\"reduce123\"]\n"
						+ "->setArg(0, <destName>Buffer)\n"
						+ "->setArg(1, variablePtr->buffer)\n"
						+ "->setArg(2, variablePtr->length)\n"
						+ "->setArg(3, <nonFinalVar>Buffer)\n"
						+ "->setWorkSize(1);\n"
						+ "});\n"
						+ "runtimePtr->runtime->submitTask(std::move(task));\n"
						+ "runtimePtr->runtime->finish();\n"
						+ "<nonFinalVar>Buffer->copyToJArray(env, PM_<nonFinalVar>);\n"
						+ "<destName>Buffer->copyToJArray(env, <destName>);\n"
						+ "}");
		st.add("destName", operation.destinationVariable.name);
		st.add("type", getTranslatedParameterType());
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
		// TODO Where is the getLength method in Array class?
	}
}
