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
import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Parameter;
import org.parallelme.compiler.intermediate.UserFunction;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.BaseTranslatorTest;
import org.parallelme.compiler.translation.SimpleTranslator;
import org.parallelme.compiler.userlibrary.classes.HDRImage;

/**
 * Performs tests to validate PMHDRImageTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public class PMHDRImageTranslatorTest extends BaseTranslatorTest {
	private String className = "SomeClass";
	private RuntimeCommonDefinitions commonDefinitions = RuntimeCommonDefinitions
			.getInstance();
	private Variable imageVar = new Variable("imageVar", "HDRImage", "", "", 1);

	private InputBind createInputBind() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", "byte[]", "", "", 2));
		parameters.add(new Variable("widthVar", "int", "", "", 3));
		parameters.add(new Variable("dataVar", "heightVar", "", "", 4));
		return new InputBind(imageVar, 1, parameters, null, null);
	}

	private OutputBind createOutputBind(OutputBindType outputBindType) {
		Variable destinationVar = new Variable("bitmapVar", "Bitmap", "", "", 1);
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", "byte[]", "", "", 2));
		parameters.add(new Variable("widthVar", "int", "", "", 3));
		parameters.add(new Variable("dataVar", "heightVar", "", "", 4));
		return new OutputBind(imageVar, destinationVar, 1, null, outputBindType);
	}

	private Operation createForeachOperation(ExecutionType executionType) {
		Operation operation = new Operation(imageVar, 123, null,
				OperationType.Foreach, null);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", "Pixel", "", "", 10));
		UserFunction userFunction = new UserFunction(
				" { param1.rgba.red = 123; }", arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}
	
	private MethodCall createMethodCall(String methodName) {
		return new MethodCall(methodName, imageVar, null, 999);
	}

	/**
	 * Tests input bind translation.
	 */
	@Test
	public void translateInputBind() throws Exception {
		String expectedTranslation = "__kernel void toFloatHDRImage(__global uchar4 *PM_dataIn, __global float4 *PM_dataOut) {"
				+ "	int PM_gid = get_global_id(0);"
				+ "	uchar4 PM_in = PM_dataIn[PM_gid];"
				+ "	float4 PM_out;"
				+ "	float PM_f;"
				+ "	if(PM_in.s3 != 0) {"
				+ "		PM_f = ldexp(1.0f, (PM_in.s3 & 0xFF) - (128 + 8));"
				+ "		PM_out.s0 = (PM_in.s0 & 0xFF) * PM_f;"
				+ "		PM_out.s1 = (PM_in.s1 & 0xFF) * PM_f;"
				+ "		PM_out.s2 = (PM_in.s2 & 0xFF) * PM_f;"
				+ "		PM_out.s3 = 0.0f;"
				+ "	} else {"
				+ "		PM_out.s0 = 0.0f;"
				+ "		PM_out.s1 = 0.0f;"
				+ "		PM_out.s2 = 0.0f;"
				+ "		PM_out.s3 = 0.0f;"
				+ "	}"
				+ "	PM_dataOut[PM_gid] = PM_out;" + "}";
		InputBind inputBind = this.createInputBind();
		PMHDRImageTranslator translator = new PMHDRImageTranslator(null);
		String translatedFunction = translator.translateInputBind(className,
				inputBind);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
	}

	/**
	 * Tests input bind object creation.
	 */
	@Test
	public void translateInputBindObjCreation() throws Exception {
		InputBind inputBind = this.createInputBind();
		PMHDRImageTranslator translator = new PMHDRImageTranslator(null);
		String translatedFunction = translator.translateInputBindObjCreation(
				className, inputBind);
		String varName = this.commonDefinitions
				.getPointerName(inputBind.variable);
		String expectedTranslation = String
				.format("%s = ParallelMERuntime.getInstance().createHDRImage(data, width, height);",
						varName);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
	}

	/**
	 * Tests input bind object declaration.
	 */
	@Test
	public void translateInputBindObjDeclaration() throws Exception {
		PMHDRImageTranslator translator = new PMHDRImageTranslator(null);
		assertEquals(translator.translateInputBindObjDeclaration(null), "");
	}

	/**
	 * Tests output bind translation.
	 */
	@Test
	public void translateOutputBind() throws Exception {
		String expectedTranslation = "__kernel void toBitmapHDRImage(__global float4 *PM_dataIn, __global uchar4 *PM_dataOut) {\n"
				+ "\tint PM_gid = get_global_id(0);\n"
				+ "\tfloat4 PM_in = PM_dataIn[PM_gid];\n"
				+ "\tuchar4 PM_out;\n"
				+ "\tPM_out.x = (uchar) (255.0f * PM_in.s0);\n"
				+ "\tPM_out.y = (uchar) (255.0f * PM_in.s1);\n"
				+ "\tPM_out.z = (uchar) (255.0f * PM_in.s2);\n"
				+ "\tPM_out.w = 255;\n"
				+ "\tPM_dataOut[PM_gid] = PM_out;\n"
				+ "}\n";
		PMHDRImageTranslator translator = new PMHDRImageTranslator(null);
		String translatedFunction = translator.translateOutputBind(className,
				null);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
	}

	/**
	 * Tests output bind call.
	 */
	@Test
	public void translateOutputBindCall() throws Exception {
		// imageVar.toBitmap(bitmapVar);
		OutputBind outputBind = this.createOutputBind(OutputBindType.None);
		String varName = this.commonDefinitions
				.getPointerName(outputBind.variable);
		String expectedTranslation = String.format(
				"ParallelMERuntime.getInstance().toBitmapHDRImage(%s, %s);",
				varName, outputBind.destinationObject);
		PMHDRImageTranslator translator = new PMHDRImageTranslator(null);
		String translatedFunction = translator.translateOutputBindCall(
				className, outputBind);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
		// bitmapVar = imageVar.toBitmap();
		outputBind = this.createOutputBind(OutputBindType.Assignment);
		expectedTranslation = String.format("%s = Bitmap.createBitmap(\n"
				+ "\tParallelMERuntime.getInstance().getWidth(%s),\n"
				+ "\tParallelMERuntime.getInstance().getHeight(%s),\n"
				+ "\tBitmap.Config.ARGB_8888);\n",
				outputBind.destinationObject, varName, varName)
				+ "\n" + expectedTranslation;
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
		// Bitmap bitmapVar = imageVar.toBitmap();
		outputBind = this
				.createOutputBind(OutputBindType.DeclarativeAssignment);
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
	}

	/**
	 * Tests operation translation.
	 */
	@Test
	public void translateOperation() throws Exception {
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		PMHDRImageTranslator translator = new PMHDRImageTranslator(
				new SimpleTranslator());
		List<String> translatedFunction = translator
				.translateOperation(operation);
		String expectedTranslation = "__kernel void foreach123(__global float4* PM_data) {"
				+ "int PM_gid = get_global_id(0);"
				+ "float4 param1 = PM_data[PM_gid];"
				+ "param1.s0 = 123; "
				+ "PM_data[PM_gid] = param1;" + "}";
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
	}

	/**
	 * Tests operation call.
	 */
	@Test
	public void translateOperationCall() throws Exception {
		Operation operation = this
				.createForeachOperation(ExecutionType.Parallel);
		PMHDRImageTranslator translator = new PMHDRImageTranslator(
				new SimpleTranslator());
		String translatedFunction = translator.translateOperationCall(
				className, operation);
		String varName = this.commonDefinitions
				.getPointerName(operation.variable);
		String expectedTranslation = String
				.format("foreach123(ParallelMERuntime.getInstance().runtimePointer, %s);",
						varName);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
	}

	/**
	 * Tests method call translation.
	 */
	@Test
	public void translateMethodCall() throws Exception {
		// getHeight method
		MethodCall methodCall = this
				.createMethodCall(HDRImage.getInstance().getHeightMethodName());
		PMHDRImageTranslator translator = new PMHDRImageTranslator(
				new SimpleTranslator());
		String translatedFunction = translator.translateMethodCall(
				className, methodCall);
		String varName = this.commonDefinitions
				.getPointerName(methodCall.variable);
		String expectedTranslation = String
				.format("return ParallelMERuntime.getInstance().%s(%s);",
						HDRImage.getInstance().getHeightMethodName(), varName);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
		methodCall = this
				.createMethodCall(HDRImage.getInstance().getWidthMethodName());
		translatedFunction = translator.translateMethodCall(
				className, methodCall);
		expectedTranslation = String
				.format("return ParallelMERuntime.getInstance().%s(%s);",
						HDRImage.getInstance().getWidthMethodName(), varName);
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedFunction));
	}
}
