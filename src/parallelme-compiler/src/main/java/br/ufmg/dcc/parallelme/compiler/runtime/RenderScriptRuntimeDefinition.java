/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import java.io.IOException;
import java.util.List;

import org.stringtemplate.v4.ST;

import br.ufmg.dcc.parallelme.compiler.runtime.translation.BoxedTypes;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.PrimitiveTypes;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClass;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.*;

/**
 * Definitions for RenderScript runtime.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class RenderScriptRuntimeDefinition extends RuntimeDefinitionImpl {
	private final String templateInitString = "RenderScript mRS = RenderScript.create(<mainClassName>.getAppContext());\n";
	private final String templateFunctionsString = "<functions:{function|ScriptC_<function> <function>_script = new ScriptC_<function>(mRS);\n}>";

	public RenderScriptRuntimeDefinition(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

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
		for (int i = firstFunctionNumber; i < firstFunctionNumber
				+ functionCount; i++) {
			st.add("functions", this.getFunctionName(i));
		}
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImports(List<UserLibraryData> iteratorsAndBinds) {
		StringBuffer imports = new StringBuffer();
		imports.append("import android.support.v8.renderscript.*;\n");
		boolean exportedHDR = false;
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (!exportedHDR
					&& userLibraryData.getVariable().typeName.equals(HDRImage
							.getName())) {
				imports.append("import br.ufmg.dcc.parallelme.userlibrary.RGBE;\n");
				exportedHDR = true;
			}
		}
		imports.append("\n");
		return imports.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String createAllocation(InputBind inputBind) {
		String ret = "";
		String inputObject = this.getVariableInName(inputBind.getVariable());
		String outputObject = this.getVariableOutName(inputBind.getVariable());
		String dataTypeInputObject = this.getPrefix() + inputBind.getVariable()
				+ "InDataType";
		String dataTypeOutputObject = this.getPrefix()
				+ inputBind.getVariable() + "OutDataType";
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.create(inputBind.getVariable().typeName);
		// If the user library class is a BitmapImage, there is only a single
		// constructor in which the parameter is a Bitmap. Thus we just get the
		// first element of the arguments' array and work with it.
		if (userLibraryClass instanceof BitmapImage) {
			ret = "Type " + dataTypeInputObject + ";\n" + inputObject
					+ " = Allocation.createFromBitmap(mRS, "
					+ inputBind.getParameters()[0] + ", "
					+ "Allocation.MipmapControl.MIPMAP_NONE, "
					+ "Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);\n"
					+ dataTypeInputObject
					+ " = new Type.Builder(mRS, Element.F32_3(mRS))" + ".setX("
					+ inputObject + ".getType().getX())" + ".setY("
					+ inputObject + ".getType().getY())" + ".create();\n"
					+ outputObject + " = Allocation.createTyped(mRS, "
					+ dataTypeInputObject + ");\n"
					+ this.getFunctionName(inputBind.sequentialNumber)
					+ "_script.forEach_root(" + inputObject + ", "
					+ outputObject + ");";
		} else if (userLibraryClass instanceof HDRImage) {
			String resourceData = this.getPrefix() + inputBind.getVariable()
					+ "ResourceData";
			StringBuilder params = new StringBuilder();
			for (int i = 0; i < inputBind.getParameters().length; i++) {
				params.append(inputBind.getParameters()[i]);
				if (i != (inputBind.getParameters().length - 1))
					params.append(",");
			}
			String resourceDataCreation = "RGBE " + resourceData
					+ " = RGBE.loadFromResource(" + params.toString() + ");\n";
			String typeInput = "Type " + dataTypeInputObject
					+ " = Type.Builder(mRS, Element.RGBA_8888(mRS))\n\t"
					+ ".setX(" + resourceData + ".width)\n\t" + ".setY("
					+ resourceData + ".height)" + "\n\t.create();\n";
			String typeOutput = "Type " + dataTypeOutputObject
					+ " = Type.Builder(mRS, Element.F32_4(mRS))\n\t" + ".setX("
					+ resourceData + ".width)" + "\n\t.setY(" + resourceData
					+ ".height)" + "\n\t.create();\n";
			String allocations = inputObject
					+ " = Allocation.createTyped(mRS, "
					+ dataTypeInputObject
					+ ", Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);\n"
					+ outputObject + " = Allocation.createTyped(mRS, "
					+ dataTypeOutputObject + ");\n" + inputObject
					+ ".copyFrom(" + resourceData + ");\n"
					+ this.getFunctionName(inputBind.sequentialNumber)
					+ "_script.forEach_root(" + inputObject + ", "
					+ outputObject + ");";
			ret = resourceDataCreation + typeInput + typeOutput + allocations;
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String declareAllocation(InputBind inputBind) {
		String ret = "";
		String inputObject = this.getVariableInName(inputBind.getVariable());
		String outputObject = this.getVariableOutName(inputBind.getVariable());
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.create(inputBind.getVariable().typeName);
		// If the user library class is a BitmapImage, there is only a single
		// constructor in which the parameter is a Bitmap. Thus we just get the
		// first element of the arguments' array and work with it.
		if (userLibraryClass instanceof BitmapImage) {
			ret = "Allocation " + inputObject + ", " + outputObject + ";\n";
		} else if (userLibraryClass instanceof HDRImage) {
			ret = "Allocation " + inputObject + ", " + outputObject + ";\n";
		}

		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String createAllocationFunction(InputBind inputBind) {
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
	public String getAllocationData(OutputBind outputBind) {
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
	public String getAllocationDataFunction(OutputBind outputBind) {
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
	public String getIterator(Variable userLibraryObject, int functionNumber) {
		return this.getFunctionName(functionNumber) + "_script.forEach_root("
				+ this.getVariableOutName(userLibraryObject) + ", "
				+ this.getVariableOutName(userLibraryObject) + ");";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateVariable(Variable variable, String code) {
		String translatedCode = "";
		if (variable.typeName.equals(RGB.getName())) {
			translatedCode = this.translateRGBVariable(variable, code);
		} else if (variable.typeName.equals(RGBA.getName())) {
			translatedCode = this.translateRGBAVariable(variable, code);
		} else if (PrimitiveTypes.isPrimitive(variable.typeName)) {
			translatedCode = code.replaceAll(variable.typeName,
					PrimitiveTypes.getCType(variable.typeName));
		} else if (BoxedTypes.isBoxed(variable.typeName)) {
			translatedCode = code.replaceAll(variable.typeName,
					BoxedTypes.getCType(variable.typeName));
		}
		return translatedCode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateType(String typeName) {
		String translatedType = "";
		if (typeName.equals(RGB.getName())) {
			translatedType = "float3";
		} else if (typeName.equals(RGBA.getName())) {
			translatedType = "float4";
		} else if (PrimitiveTypes.isPrimitive(typeName)) {
			translatedType = PrimitiveTypes.getCType(typeName);
		} else if (BoxedTypes.isBoxed(typeName)) {
			translatedType = BoxedTypes.getCType(typeName);
		}
		return translatedType;
	}

	private String translateRGBVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".red", variable.name + ".s0");
		ret = ret.replaceAll(variable.name + ".green", variable.name + ".s1");
		ret = ret.replaceAll(variable.name + ".blue", variable.name + ".s2");
		return ret;
	}

	private String translateRGBAVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".red", variable.name + ".s0");
		ret = ret.replaceAll(variable.name + ".green", variable.name + ".s1");
		ret = ret.replaceAll(variable.name + ".blue", variable.name + ".s2");
		ret = ret.replaceAll(variable.name + ".alpha", variable.name + ".s3");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getIteratorFunctionSignature(Iterator iterator) {
		String functionSignature = "";
		String parameterTypeTranslated = this.translateType(iterator
				.getUserFunctionData().variableArgument.typeName);
		if (iterator.getVariable().typeName.equals(BitmapImage.getName())) {
			functionSignature = parameterTypeTranslated
					+ " __attribute__((kernel)) root("
					+ parameterTypeTranslated + " "
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

	/**
	 * Not necessary for RenderScript runtime.
	 */
	@Override
	public void exportInternalLibrary(String packageName,
			String destinationFolder) throws IOException {
		// Copy all files and directories under ParallelME resource folder to
		// the destination folder.
		// String resourceName = "RenderScript";
	}
}
