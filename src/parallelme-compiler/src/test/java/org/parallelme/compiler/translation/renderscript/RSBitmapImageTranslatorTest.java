/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

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
import org.stringtemplate.v4.ST;

/**
 * Performs tests to validate PMBitmapImageTranslator class.
 * 
 * @author Wilson de Carvalho
 */
public class RSBitmapImageTranslatorTest extends RSImageTranslatorTest {
	private Variable imageVar = new Variable("imageVar", "BitmapImage", "", "",
			1);

	@Override
	protected Variable getUserLibraryVar() {
		return this.imageVar;
	}

	@Override
	protected BaseUserLibraryTranslator getTranslator() {
		return new RSBitmapImageTranslator(new SimpleTranslator());
	}

	private InputBind createInputBind() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", "Bitmap", "", "", 2));
		parameters.add(new Variable("widthVar", "int", "", "", 3));
		parameters.add(new Variable("heightVar", "in", "", "", 4));
		return new InputBind(this.getUserLibraryVar(), 1, parameters, null,
				null);
	}

	private OutputBind createOutputBind(OutputBindType outputBindType) {
		Variable destinationVar = new Variable("bitmapVar", "Bitmap", "", "", 1);
		return new OutputBind(this.getUserLibraryVar(), destinationVar, 1,
				null, outputBindType);
	}

	/**
	 * Tests input bind translation.
	 */
	@Test
	public void translateInputBind() throws Exception {
		String expectedTranslation = "float3 __attribute__ ((kernel)) toFloatBitmapImage(uchar4 PM_in, uint32_t x, uint32_t y) {\n"
				+ "float3 PM_out;\n"
				+ "PM_out.s0 = (float)PM_in.r;\n"
				+ "PM_out.s1 = (float)PM_in.g;\n"
				+ "PM_out.s2 = (float)PM_in.b;\n" + "return PM_out;\n" + "}";
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
		ST st = new ST(
				"Type <varIn>DataType;\n"
						+ "<varIn> = Allocation.createFromBitmap(PM_mRS, dataVar, Allocation.MipmapControl.MIPMAP_NONE, "
						+ "Allocation.USAGE_SCRIPT|Allocation.USAGE_SHARED);\n"
						+ "<varIn>DataType = newType.Builder(PM_mRS, Element.F32_3(PM_mRS))"
						+ ".setX(<varIn>.getType().getX())"
						+ ".setY(<varIn>.getType().getY())"
						+ ".create();\n"
						+ "<varOut> = Allocation.createTyped(PM_mRS, <varIn>DataType);\n"
						+ "<kernel>.forEach_toFloatBitmapImage(<varIn>, <varOut>);");
		st.add("kernel", commonDefinitions.getKernelName(className));
		st.add("varIn", commonDefinitions.getVariableInName(inputBind.variable));
		st.add("varOut",
				commonDefinitions.getVariableOutName(inputBind.variable));
		String expectedTranslation = st.render();
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests input bind object declaration.
	 */
	@Test
	public void translateInputBindObjDeclaration() throws Exception {
		BaseUserLibraryTranslator translator = this.getTranslator();
		InputBind inputBind = createInputBind();
		ST st = new ST("private Allocation <varIn>, <varOut>;");
		st.add("varIn", commonDefinitions.getVariableInName(inputBind.variable));
		st.add("varOut",
				commonDefinitions.getVariableOutName(inputBind.variable));
		String expectedTranslation = st.render();
		String translatedFunction = translator
				.translateInputBindObjDeclaration(inputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests output bind translation.
	 */
	@Test
	public void translateOutputBind() throws Exception {
		String expectedTranslation = "uchar4 __attribute__ ((kernel)) toBitmapBitmapImage(float3 PM_in, uint32_t x,uint32_t y) {\n"
				+ "uchar4 PM_out;\n"
				+ "PM_out.r = (uchar)(PM_in.s0);\n"
				+ "PM_out.g = (uchar)(PM_in.s1);\n"
				+ "PM_out.b = (uchar)(PM_in.s2);\n"
				+ "PM_out.a = 255;"
				+ "return PM_out;\n" + "}";
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
		String kernelName = this.commonDefinitions.getKernelName(className);
		String varInName = this.commonDefinitions
				.getVariableInName(outputBind.variable);
		String varOutName = this.commonDefinitions
				.getVariableOutName(outputBind.variable);
		ST st = new ST(
				"<kernel>.forEach_toBitmapBitmapImage(<varOut>,<varIn>);"
						+ "<varIn>.copyTo(<varDest>);");
		st.add("kernel", kernelName);
		st.add("varIn", varInName);
		st.add("varOut", varOutName);
		st.add("varDest", outputBind.destinationObject);
		String expectedTranslation = st.render();
		BaseUserLibraryTranslator translator = this.getTranslator();
		String translatedFunction = translator.translateOutputBindCall(
				className, outputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
		// bitmapVar = imageVar.toBitmap();
		outputBind = this.createOutputBind(OutputBindType.Assignment);
		st = new ST(
				"<varDest>=Bitmap.createBitmap(<varIn>.getType().getX(), <varIn>.getType().getY(), "
						+ "Bitmap.Config.ARGB_8888);"
						+ "<kernel>.forEach_toBitmapBitmapImage(<varOut>,<varIn>);"
						+ "<varIn>.copyTo(<varDest>);");
		st.add("kernel", kernelName);
		st.add("varIn", varInName);
		st.add("varOut", varOutName);
		st.add("varDest", outputBind.destinationObject);
		expectedTranslation = st.render();
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
