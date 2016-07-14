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
public class RSHDRImageTranslatorTest extends RSImageTranslatorTest {
	private Variable imageVar = new Variable("imageVar", "HDRImage", null, "",
			1);

	@Override
	protected Variable getUserLibraryVar() {
		return this.imageVar;
	}

	@Override
	protected BaseUserLibraryTranslator getTranslator() {
		return new RSHDRImageTranslator(new SimpleTranslator());
	}

	@Override
	protected String getRSType() {
		return "F32_4";
	}

	@Override
	protected InputBind createInputBind() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", "byte[]", null, "", 2));
		parameters.add(new Variable("widthVar", "int", null, "", 3));
		parameters.add(new Variable("heightVar", "int", null, "", 4));
		return new InputBind(this.getUserLibraryVar(), 1, parameters, null,
				null);
	}

	@Override
	protected OutputBind createOutputBind(OutputBindType outputBindType) {
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
		String expectedTranslation = "float4 __attribute__ ((kernel)) toFloatHDRImage(uchar4 PM_in, uint32_t x, uint32_t y) {\n"
				+ "float4 PM_out;\n"
				+ "if (PM_in.s3!=0) {\n"
				+ "float f = ldexp(1.0f,(PM_in.s3 & 0xFF)-(128+8));\n"
				+ "PM_out.s0 = (PM_in.s0 & 0xFF)*f;\n"
				+ "PM_out.s1 = (PM_in.s1 & 0xFF)*f;\n"
				+ "PM_out.s2 = (PM_in.s2 & 0xFF)*f;\n"
				+ "PM_out.s3 = 0.0f;\n"
				+ "} else {\n"
				+ "PM_out.s0 = 0.0f;\n"
				+ "PM_out.s1 = 0.0f;\n"
				+ "PM_out.s2 = 0.0f;\n"
				+ "PM_out.s3 = 0.0f;\n"
				+ "}\n"
				+ "return PM_out;\n" + "}";
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
				"Type <varIn>DataType = newType.Builder(PM_mRS, Element.RGBA_8888(PM_mRS))"
						+ ".setX(width)"
						+ ".setY(height)"
						+ ".create();\n"
						+ "Type <varOut>DataType = newType.Builder(PM_mRS,Element.F32_4(PM_mRS))"
						+ ".setX(width)"
						+ ".setY(height)"
						+ ".create();\n"
						+ "<varIn> = Allocation.createTyped(PM_mRS, <varIn>DataType,"
						+ "Allocation.MipmapControl.MIPMAP_NONE,"
						+ "Allocation.USAGE_SCRIPT);\n"
						+ "<varOut>=Allocation.createTyped(PM_mRS, <varOut>DataType);\n"
						+ "<varIn>.copyFrom(data);\n"
						+ "<kernel>.forEach_toFloatHDRImage(<varIn>, <varOut>);");
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
	public void translateObjDeclaration() throws Exception {
		BaseUserLibraryTranslator translator = this.getTranslator();
		InputBind inputBind = createInputBind();
		ST st = new ST("private Allocation <varIn>, <varOut>;");
		st.add("varIn", commonDefinitions.getVariableInName(inputBind.variable));
		st.add("varOut",
				commonDefinitions.getVariableOutName(inputBind.variable));
		String expectedTranslation = st.render();
		String translatedFunction = translator
				.translateObjDeclaration(inputBind);
		this.validateTranslation(expectedTranslation, translatedFunction);
	}

	/**
	 * Tests output bind translation.
	 */
	@Test
	public void translateOutputBind() throws Exception {
		String expectedTranslation = "uchar4 __attribute__ ((kernel)) toBitmapHDRImage(float4 PM_in, uint32_t x,uint32_t y) {\n"
				+ "uchar4 PM_out;\n"
				+ "PM_out.r = (uchar)(PM_in.s0*255.0f);\n"
				+ "PM_out.g = (uchar)(PM_in.s1*255.0f);\n"
				+ "PM_out.b = (uchar)(PM_in.s2*255.0f);\n"
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
		ST st = new ST("<kernel>.forEach_toBitmapHDRImage(<varOut>,<varIn>);"
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
						+ "<kernel>.forEach_toBitmapHDRImage(<varOut>,<varIn>);"
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
