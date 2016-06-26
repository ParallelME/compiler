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

/**
 * Performs tests to validate PMArrayTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public class PMArrayTranslatorTest extends BaseTranslatorTest {
	private Variable arrayVar = new Variable("arrayVar", "Array", "", "", 1);

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
		parameters.add(new Variable("dataVar", "int[]", "", "", 2));
		parameters.add(new Variable("Int32.class", "Class", "Int32", "", 3));
		return new InputBind(this.getUserLibraryVar(), 1, parameters, null,
				null);
	}

	private OutputBind createOutputBind(OutputBindType outputBindType) {
		Variable destinationVar = new Variable("arrayVar", "int[]", "", "", 1);
		return new OutputBind(this.getUserLibraryVar(), destinationVar, 1,
				null, outputBindType);
	}

	private Operation createForeachOperation(ExecutionType executionType) {
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Foreach, null);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", "Int32", "", "", 10));
		UserFunction userFunction = new UserFunction(
				" { param1.value = 123; }", arguments);
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
		String expectedTranslation = "__kernel void foreach123(__global int* PM_data) {"
				+ "int PM_gid = get_global_id(0);"
				+ "int param1 = PM_data[PM_gid];"
				+ "param1 = 123; "
				+ "PM_data[PM_gid] = param1;" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final");
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String.format(
				"__kernel void foreach123(__global int* PM_data, %s %s) {"
						+ "int PM_gid = get_global_id(0);"
						+ "int param1 = PM_data[PM_gid];" + "param1 = 123; "
						+ "PM_data[PM_gid] = param1;" + "}", finalVar.typeName,
				finalVar.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with non-final external variable (will be translated to
		// sequential code)
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable nonFinalVar = this.createExternalVariable("");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("__kernel void foreach123(__global int *PM_data, %s %s, __global %s* %s, int PM_length) {"
						+ "for(int PM_x=0; PM_x<PM_length; ++PM_x) {"
						+ "int param1 = PM_data[PM_x];"
						+ "param1 = 123;"
						+ "PM_data[PM_x] = param1;" + "}*%s=%s;}",
						nonFinalVar.typeName, nonFinalVar.name,
						nonFinalVar.typeName,
						this.commonDefinitions.getPrefix() + nonFinalVar.name,
						this.commonDefinitions.getPrefix() + nonFinalVar.name,
						nonFinalVar.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential
		operation = this.createForeachOperation(ExecutionType.Sequential);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = "__kernel void foreach123(__global int *PM_data, int PM_length) {"
				+ "for(int PM_x=0; PM_x<PM_length; ++PM_x) {"
				+ "int param1 = PM_data[PM_x];"
				+ "param1 = 123;"
				+ "PM_data[PM_x] = param1;" + "}}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(finalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String.format(
				"__kernel void foreach123(__global int *PM_data, %s %s, int PM_length) {"
						+ "for(int PM_x=0; PM_x<PM_length; ++PM_x) {"
						+ "int param1 = PM_data[PM_x];" + "param1 = 123;"
						+ "PM_data[PM_x] = param1;" + "}}", finalVar.typeName,
				finalVar.name);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Sequential with non-final variable
		operation = this.createForeachOperation(ExecutionType.Sequential);
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("__kernel void foreach123(__global int *PM_data, %s %s, __global %s* %s, int PM_length) {"
						+ "for(int PM_x=0; PM_x<PM_length; ++PM_x) {"
						+ "int param1 = PM_data[PM_x];"
						+ "param1 = 123;"
						+ "PM_data[PM_x] = param1;" + "}*%s=%s;}",
						nonFinalVar.typeName, nonFinalVar.name,
						nonFinalVar.typeName,
						this.commonDefinitions.getPrefix() + nonFinalVar.name,
						this.commonDefinitions.getPrefix() + nonFinalVar.name,
						nonFinalVar.name);
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
	 * Tests method call translation.
	 */
	@Test
	public void translateMethodCall() throws Exception {
		// TODO Where is the getLength method in Array class?
	}
}
