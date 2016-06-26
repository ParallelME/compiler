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
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;

/**
 * Performs tests to validate PMBitmapImageTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public class PMBitmapImageTranslatorTest extends PMImageTranslatorTest {
	private Variable imageVar = new Variable("imageVar", "BitmapImage", "", "",
			1);

	@Override
	protected Variable getUserLibraryVar() {
		return this.imageVar;
	}

	@Override
	protected BaseUserLibraryTranslator getTranslator() {
		return new PMBitmapImageTranslator(new SimpleTranslator());
	}

	private InputBind createInputBind() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", "Bitmap", "", "", 2));
		parameters.add(new Variable("widthVar", "int", "", "", 3));
		parameters.add(new Variable("heightVar", "in", "", "", 4));
		return new InputBind(this.getUserLibraryVar(), 1, parameters, null, null);
	}

	private OutputBind createOutputBind(OutputBindType outputBindType) {
		Variable destinationVar = new Variable("bitmapVar", "Bitmap", "", "", 1);
		return new OutputBind(this.getUserLibraryVar(), destinationVar, 1, null, outputBindType);
	}

	/**
	 * Tests input bind translation.
	 */
	@Test
	public void translateInputBind() throws Exception {
		String expectedTranslation = "__kernel void toFloatBitmapImage(__global uchar4 *PM_dataIn, __global float4 *PM_dataOut) {\n"
				+ "\tint PM_gid = get_global_id(0);\n"
				+ "\tuchar4 PM_in = PM_dataIn[PM_gid];\n"
				+ "\tfloat3 PM_out;\n"
				+ "\tPM_out.s0 = (float) PM_in.s0;\n"
				+ "\tPM_out.s1 = (float) PM_in.s1;\n"
				+ "\tPM_out.s2 = (float) PM_in.s2;\n"
				+ "\tPM_out.s3 = 0f;\n"
				+ "\tPM_dataOut[PM_gid] = PM_out;\n" + "}\n";
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
				"%s = ParallelMERuntime.getInstance().createBitmapImage(%s);",
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
		String expectedTranslation = "__kernel void toBitmapBitmapImage(__global float4 *PM_dataIn, __global uchar4 *PM_dataOut) {\n"
				+ "\tint PM_gid = get_global_id(0);\n"
				+ "\tfloat4 PM_in = PM_dataIn[PM_gid];\n"
				+ "\tuchar4 PM_out;\n"
				+ "\tPM_out.x = (uchar) PM_in.s0;\n"
				+ "\tPM_out.y = (uchar) PM_in.s1;\n"
				+ "\tPM_out.z = (uchar) PM_in.s2;\n"
				+ "\tPM_out.w = 255;\n"
				+ "\tPM_dataOut[PM_gid] = PM_out;\n" + "}\n";
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
		// imageVar.toBitmap(bitmapVar);
		OutputBind outputBind = this.createOutputBind(OutputBindType.None);
		String varName = this.commonDefinitions
				.getPointerName(outputBind.variable);
		String expectedTranslation = String.format(
				"ParallelMERuntime.getInstance().toBitmapBitmapImage(%s, %s);",
				varName, outputBind.destinationObject);
		BaseUserLibraryTranslator translator = this.getTranslator();
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
