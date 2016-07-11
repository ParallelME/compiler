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
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Parameter;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.SimpleTranslator;

/**
 * Performs tests to validate PMHDRImageTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public class PMHDRImageTranslatorTest extends PMImageTranslatorTest {
	private Variable imageVar = new Variable("imageVar", "HDRImage", null, "",
			1);

	@Override
	protected Variable getUserLibraryVar() {
		return this.imageVar;
	}

	@Override
	protected PMTranslator getTranslator() {
		return new PMHDRImageTranslator(new SimpleTranslator());
	}

	private InputBind createInputBind() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", "byte[]", null, "", 2));
		parameters.add(new Variable("widthVar", "int", null, "", 3));
		parameters.add(new Variable("heightVar", "int", null, "", 4));
		return new InputBind(this.getUserLibraryVar(), 1, parameters, null,
				null);
	}

	private OutputBind createOutputBind(OutputBindType outputBindType) {
		Variable destinationVar = new Variable("bitmapVar", "Bitmap", null, "",
				1);
		return new OutputBind(this.getUserLibraryVar(), destinationVar, 1,
				null, outputBindType);
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
		PMTranslator translator = this.getTranslator();
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
		PMTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateInputBindObjCreation(
				className, inputBind);
		String varName = this.commonDefinitions
				.getPointerName(inputBind.variable);
		String expectedTranslation = String
				.format("%s = ParallelMERuntime.getInstance().createHDRImage(data, width, height);",
						varName);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests input bind object declaration.
	 */
	@Test
	public void translateInputBindObjDeclaration() throws Exception {
		PMTranslator translator = this.getTranslator();
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
		PMTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBind(className,
				null);
		this.validateTranslation(expectedTranslation, translatedFunction);
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
		PMTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBindCall(
				className, outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
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
		this.validateTranslation(expectedTranslation, translatedFunction);
		// Bitmap bitmapVar = imageVar.toBitmap();
		outputBind = this
				.createOutputBind(OutputBindType.DeclarativeAssignment);
		translatedFunction = translator.translateOutputBindCall(className,
				outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}
}
