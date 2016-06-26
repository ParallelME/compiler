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
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.UserFunction;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.BaseTranslatorTest;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;

/**
 * Base class for image tests.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMImageTranslatorTest extends BaseTranslatorTest {
	protected Operation createForeachOperation(ExecutionType executionType) {
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Foreach, null);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", "Pixel", "", "", 10));
		UserFunction userFunction = new UserFunction(
				" { param1.rgba.red = 123; }", arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
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
		String expectedTranslation = "__kernel void foreach123(__global float4* PM_data) {"
				+ "int PM_gid = get_global_id(0);"
				+ "float4 param1 = PM_data[PM_gid];"
				+ "param1.s0 = 123; "
				+ "PM_data[PM_gid] = param1;" + "}";
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Parallel with final external variable
		operation = this.createForeachOperation(ExecutionType.Parallel);
		Variable finalVar = this.createExternalVariable("final");
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
		Variable nonFinalVar = this.createExternalVariable("");
		operation.addExternalVariable(nonFinalVar);
		translatedFunction = translator.translateOperation(operation);
		expectedTranslation = String
				.format("__kernel void foreach123(__global float4 *PM_data, %s %s, __global %s* %s, int PM_width, int PM_height) {"
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
				.format("__kernel void foreach123(__global float4 *PM_data, %s %s, int PM_width, int PM_height) {"
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
				.format("__kernel void foreach123(__global float4 *PM_data, %s %s, __global %s* %s, int PM_width, int PM_height) {"
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
