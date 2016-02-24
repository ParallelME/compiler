/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import org.stringtemplate.v4.ST;

import br.ufmg.dcc.parallelme.compiler.runtime.translationdata.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClass;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.BitmapImage;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.RGBA;

/**
 * Definitions for RenderScript runtime.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class RenderScriptRuntimeDefinition extends RuntimeDefinitionImpl {
	private final String templateInitString = "RenderScript mRS = RenderScript.create(<mainClassName>.getAppContext());\n";
	private final String templateFunctionsString = "<functions:{function|ScriptC_<function> <function>_script = new ScriptC_<function>(mRS);\n}>";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInitializationString() {
		ST st = new ST(templateInitString);
		st.add("mainClassName", "MainActivity"); // TODO: I'm cheating
													// here, I can't
													// know this class
													// name yet.
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFunctionInitializationString(int firstFunctionNumber,
			int functionCount) {
		ST st = new ST(templateFunctionsString);
		for (int i = firstFunctionNumber; i < functionCount; i++) {
			st.add("functions", this.getFunctionName(i));
		}
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImports() {
		return "import android.support.v8.renderscript.*;\n\n";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCreateAllocationString(InputBind inputBind) {
		String allocationString = "";
		String inputObject = this.getVariableInName(inputBind.getVariable());
		String outputObject = this.getVariableOutName(inputBind.getVariable());
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.create(inputBind.getVariable().typeName);
		// If the user library class is a BitmapImage, there is only a single
		// constructor in which the parameter is a Bitmap. Thus we just get the
		// first element of the arguments' array and work with it.
		if (userLibraryClass instanceof BitmapImage) {
			allocationString = "Allocation " + inputObject + ", "
					+ outputObject + ";\n" + "Type dataType;\n" + inputObject
					+ " = Allocation.createFromBitmap(mRS, "
					+ inputBind.getParameters()[0] + ", "
					+ "Allocation.MipmapControl.MIPMAP_NONE, "
					+ "Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);\n"
					+ "dataType = new Type.Builder(mRS, Element.F32_3(mRS))"
					+ ".setX(" + inputObject + ".getType().getX())" + ".setY("
					+ inputObject + ".getType().getY())" + ".create();\n"
					+ outputObject
					+ " = Allocation.createTyped(mRS, dataType);\n"
					+ this.getFunctionName(inputBind.sequentialNumber)
					+ "_script.forEach_root(" + inputObject + ", "
					+ outputObject + ");";
		}
		return allocationString;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCreateAllocationFunction(InputBind inputBind) {
		return "float3 __attribute__((kernel)) root(uchar4 in, uint32_t x, uint32_t y) {"
				+ "\nfloat3 out;"
				+ "\nout.s0 = ((float) in.r) / 255.0f;"
				+ "\nout.s1 = ((float) in.g) / 255.0f;"
				+ "\nout.s2 = ((float) in.b) / 255.0f;"
				+ "\nreturn out;"
				+ "\n}";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAllocationDataOutputString(OutputBind outputBind) {
		String inputObject = this.getVariableInName(outputBind.getVariable());
		String outputObject = this.getVariableOutName(outputBind.getVariable());
		return this.getFunctionName(outputBind.sequentialNumber)
				+ "_script.forEach_root(" + outputObject + ", " + inputObject
				+ ");\n" + inputObject + ".copyTo("
				+ outputBind.getDestinationObject().name + ");\n";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAllocationDataOutputFunction(OutputBind outputBind) {
		return "uchar4 __attribute__((kernel)) root(float3 in, uint32_t x, uint32_t y) {"
				+ "\nuchar4 out;"
				+ "\nout.r = (uchar) (in.s0 * 255.0f);"
				+ "\nout.g = (uchar) (in.s1 * 255.0f);"
				+ "\nout.b = (uchar) (in.s2 * 255.0f);"
				+ "\nout.a = 255;"
				+ "\nreturn out;" + "\n}";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIteratorString(Variable userLibraryObject,
			int functionNumber) {
		return this.getFunctionName(functionNumber) + "_script.forEach_root("
				+ this.getVariableOutName(userLibraryObject) + ", "
				+ this.getVariableOutName(userLibraryObject) + ");";
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateUserLibraryType(Variable userLibraryObject,
			String code) {
		String translatedCode = "";
		if (userLibraryObject.typeName.equals(RGBA.getName())) {
			translatedCode = this.translateRGBAType(userLibraryObject, code);
		}
		return translatedCode;
	}

	private String translateRGBAType(Variable userLibraryObject, String code) {
		String ret = code.replaceAll(userLibraryObject.name + ".red",
				userLibraryObject.name + ".s0");
		ret = ret.replaceAll(userLibraryObject.name + ".green",
				userLibraryObject.name + ".s1");
		ret = ret.replaceAll(userLibraryObject.name + ".blue",
				userLibraryObject.name + ".s2");
		ret = ret.replaceAll(userLibraryObject.name + ".alpha",
				userLibraryObject.name + ".s3");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getIteratorFunctionSignature(Iterator iterator) {
		String functionSignature = "";
		if (iterator.getVariable().typeName.equals(BitmapImage.getName())) {
			functionSignature = "float3 __attribute__((kernel)) root(float3 "
					+ iterator.getUserFunctionData().variableArgument.name
					+ ", uint32_t x, uint32_t y)";
		}
		return functionSignature;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCFileExtension() {
		return "rs";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCFunctionHeader(String packageName) {
		return "#pragma version(1)" + "\n#pragma rs java_package_name("
				+ packageName + ")\n";
	}
}
