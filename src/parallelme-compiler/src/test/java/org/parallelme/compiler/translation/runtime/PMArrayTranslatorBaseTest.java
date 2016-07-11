/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.UserFunction;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Parameter;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.BaseTranslatorTest;
import org.parallelme.compiler.translation.SimpleTranslator;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.stringtemplate.v4.ST;

/**
 * Performs tests to validate PMArrayTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMArrayTranslatorBaseTest extends BaseTranslatorTest {
	abstract protected String getParameterType();

	abstract protected String getTranslatedParameterType();

	private Variable arrayVar = new Variable("arrayVar", "Array",
			Arrays.asList(getParameterType()), "", 1);

	@Override
	protected Variable getUserLibraryVar() {
		return this.arrayVar;
	}

	@Override
	protected BaseUserLibraryTranslator getTranslator() {
		return new PMArrayTranslator(new SimpleTranslator());
	}

	private InputBind createInputBind() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", getTranslatedParameterType()
				+ "[]", null, "", 2));
		parameters.add(new Variable(getParameterType() + ".class", "Class",
				Arrays.asList(getParameterType()), "", 3));
		return new InputBind(this.getUserLibraryVar(), 1, parameters, null,
				null);
	}

	private OutputBind createOutputBind(OutputBindType outputBindType) {
		Variable destinationVar = new Variable("arrayVar",
				getTranslatedParameterType() + "[]", null, "", 1);
		return new OutputBind(this.getUserLibraryVar(), destinationVar, 1,
				null, outputBindType);
	}

	private Operation createForeachOperation(ExecutionType executionType) {
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Foreach, null);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", getParameterType(), null, "", 10));
		UserFunction userFunction = new UserFunction(
				" { param1.value = 123; }", arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}

	protected Operation createReduceOperation(ExecutionType executionType) {
		Variable destVar = new Variable("destVar", getParameterType(), null,
				"", 999);
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Reduce, destVar);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", getParameterType(), null, "", 10));
		arguments.add(new Variable("param2", getParameterType(), null, "", 11));
		UserFunction userFunction = new UserFunction(
				" { param2.value += param1.value; " + "return param2;}",
				arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
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
	 * Tests input bind object declaration.
	 */
	@Test
	public void translateInputBindObjDeclaration() throws Exception {
		BaseUserLibraryTranslator translator = this.getTranslator();
		assertEquals(translator.translateInputBindObjDeclaration(null), "");
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
		String expectedTranslation = String.format(
				"ParallelMERuntime.getInstance().toArray(%s, %s);", varName,
				outputBind.destinationObject);
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBindCall(
				className, outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// bitmapVar = imageVar.toBitmap();
		outputBind = this.createOutputBind(OutputBindType.Assignment);
		expectedTranslation = String
				.format("%s = (%s) java.lang.reflect.Array.newInstance(%s.class, "
						+ "\tParallelMERuntime.getInstance().getLength(%s));\n",
						outputBind.destinationObject,
						outputBind.destinationObject.typeName,
						outputBind.destinationObject.typeName.replaceAll("\\[",
								"").replaceAll("\\]", ""), varName)
				+ "\n" + expectedTranslation;
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Bitmap bitmapVar = imageVar.toBitmap();
		outputBind = this
				.createOutputBind(OutputBindType.DeclarativeAssignment);
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
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
		ST st = new ST("__kernel void foreach123(__global <type>* PM_data) {"
				+ "int PM_gid = get_global_id(0);"
				+ "<type> param1 = PM_data[PM_gid];" + "param1 = 123; "
				+ "PM_data[PM_gid] = param1;" + "}");
		st.add("type", getTranslatedParameterType());
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"__kernel void foreach123(__global <type>* PM_data, <finalVarType> <finalVar>) {"
						+ "int PM_gid = get_global_id(0);"
						+ "<type> param1 = PM_data[PM_gid];"
						+ "param1 = 123; "
						+ "PM_data[PM_gid] = param1;" + "}");
		st.add("type", getTranslatedParameterType());
		st.add("finalVarType", finalVar.typeName);
		st.add("finalVar", finalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"__kernel void foreach123(__global <type> *PM_data, int PM_length, <nonFinalVarType> <nonFinalVar>, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_length; ++PM_x) {"
						+ "<type> param1 = PM_data[PM_x];"
						+ "param1 = 123;"
						+ "PM_data[PM_x] = param1;"
						+ "}*PM_<nonFinalVar>=<nonFinalVar>;}");
		st.add("type", getTranslatedParameterType());
		st.add("nonFinalVarType", nonFinalVar.typeName);
		st.add("nonFinalVar", nonFinalVar.name);
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"__kernel void foreach123(__global <type> *PM_data, int PM_length) {"
						+ "for(int PM_x=0; PM_x\\<PM_length; ++PM_x) {"
						+ "<type> param1 = PM_data[PM_x];" + "param1 = 123;"
						+ "PM_data[PM_x] = param1;" + "}}");
		st.add("type", getTranslatedParameterType());
		expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"__kernel void foreach123(__global <type> *PM_data, int PM_length, <finalVarType> <finalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_length; ++PM_x) {"
						+ "<type> param1 = PM_data[PM_x];"
						+ "param1 = 123;"
						+ "PM_data[PM_x] = param1;" + "}}");
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
				"__kernel void foreach123(__global <type> *PM_data, int PM_length, <nonFinalVarType> <nonFinalVar>, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "for(int PM_x=0; PM_x\\<PM_length; ++PM_x) {"
						+ "<type> param1 = PM_data[PM_x];"
						+ "param1 = 123;"
						+ "PM_data[PM_x] = param1;"
						+ "}*PM_<nonFinalVar>=<nonFinalVar>;}");
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
				"<type> reduce123_func(<type> param1, <type> param2) {\n"
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
		Variable finalVar = this.createExternalVariable("final");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"<type> reduce123_func(<type> param1, <type> param2, <finalVarType> <finalVar>) {\n"
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
		Variable nonFinalVar = this.createExternalVariable("");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		st = new ST(
				"<type> reduce123_func(<type> param1, <type> param2, <nonFinalVarType> <nonFinalVar>, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "param2 += param1;"
						+ "return param2;} \n"
						+ "__kernel void reduce123(__global <type>* PM_destVar, __global <type>* PM_data, int PM_length, <nonFinalVarType> <nonFinalVar>, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "<type> param1 = PM_data[0];"
						+ "<type> param2;"
						+ "for (int PM_x=1; PM_x\\<PM_length; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1, param2, <nonFinalVar>, PM_<nonFinalVar>);"
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
				"<type> reduce123_func(<type> param1, <type> param2) {\n"
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
				"<type> reduce123_func(<type> param1, <type> param2, <finalVarType> <finalVar>) {\n"
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
				"<type> reduce123_func(<type> param1, <type> param2, <nonFinalVarType> <nonFinalVar>, __global <nonFinalVarType>* PM_<nonFinalVar>) {\n"
						+ "param2 += param1;"
						+ "return param2;} \n"
						+ "__kernel void reduce123(__global <type>* PM_destVar, __global <type>* PM_data, int PM_length, <nonFinalVarType> <nonFinalVar>, __global <nonFinalVarType>* PM_<nonFinalVar>) {"
						+ "<type> param1 = PM_data[0];"
						+ "<type> param2;"
						+ "for (int PM_x=1; PM_x\\<PM_length; ++PM_x) {"
						+ "param2 = PM_data[PM_x];"
						+ "param1 = reduce123_func(param1, param2, <nonFinalVar>, PM_<nonFinalVar>);"
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
	 * Tests method call translation.
	 */
	@Test
	public void translateMethodCall() throws Exception {
		// TODO Where is the getLength method in Array class?
	}
}
