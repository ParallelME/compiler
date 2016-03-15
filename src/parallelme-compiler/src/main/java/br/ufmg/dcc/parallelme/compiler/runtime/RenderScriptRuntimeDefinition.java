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

import br.ufmg.dcc.parallelme.compiler.SimpleLogger;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.BoxedTypes;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.PrimitiveTypes;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Iterator.IteratorType;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClass;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.*;

/**
 * Definitions for RenderScript runtime.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class RenderScriptRuntimeDefinition extends RuntimeDefinitionImpl {
	private static final String templateFunctions = "\t<functions:{function|ScriptC_<function> <function>_script;\n}>";
	private static final String templateConstructor = "\tpublic <ClassName>(RenderScript mRS) {\n\t\tthis.mRS = mRS;\n\t\t<functions:{function|this.<function>_script = new ScriptC_<function>(mRS);\n}>\t}\n";
	private static final String templateCreateAllocationBitmapImage = "Type <dataTypeInputObject>;\n"
			+ "<inputObject> = Allocation.createFromBitmap(mRS, <param>, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);\n"
			+ "<dataTypeInputObject> = new Type.Builder(mRS, Element.F32_3(mRS))\n"
			+ "\t.setX(<inputObject>.getType().getX())\n"
			+ "\t.setY(<inputObject>.getType().getY())\n"
			+ "\t.create();\n"
			+ "<outputObject> = Allocation.createTyped(mRS, <dataTypeInputObject>);\n"
			+ "<functionName>_script.forEach_root(<inputObject>, <outputObject>);";
	private static final String templateCreateAllocationHDRImage = "RGBE.ResourceData <resourceData> = RGBE.loadFromResource(<params>);\n"
			+ "Type <dataTypeInputObject> = new Type.Builder(mRS, Element.RGBA_8888(mRS))\n"
			+ "\t.setX(<resourceData>.width)\n"
			+ "\t.setY(<resourceData>.height)\n"
			+ "\t.create();\n"
			+ "Type <dataTypeOutputObject> = new Type.Builder(mRS, Element.F32_4(mRS))\n"
			+ "\t.setX(<resourceData>.width)\n"
			+ "\t.setY(<resourceData>.height)\n"
			+ "\t.create();\n"
			+ "<inputObject> = Allocation.createTyped(mRS, <dataTypeInputObject>, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);\n"
			+ "<outputObject> = Allocation.createTyped(mRS, <dataTypeOutputObject>);\n"
			+ "<inputObject>.copyFrom(<resourceData>.data);\n"
			+ "<functionName>_script.forEach_root(<inputObject>, <outputObject>);";
	private static final String templateAllocationDataFunctionBitmapHDRImage = "uchar4 __attribute__((kernel)) root(<varType>"
			+ " in, uint32_t x, uint32_t y) {"
			+ "\n\tuchar4 out;"
			+ "\n\tout.r = (uchar) (in.s0 * 255.0f);"
			+ "\n\tout.g = (uchar) (in.s1 * 255.0f);"
			+ "\n\tout.b = (uchar) (in.s2 * 255.0f);"
			+ "\n\tout.a = 255;"
			+ "\n\treturn out;\n}";
	private static final String templateIteratorParallelFunctionSignature = "<parameterTypeTranslated> __attribute__((kernel)) root(<parameterTypeTranslated> <userFunctionName>, uint32_t x, uint32_t y)";
	private static final String templateIteratorSequentialFunctionSignature = "void <functionName>()";
	private static final String templateIteratorSequentialFunction = "rs_allocation <inputData>;\n"
			+ "<outVariable:{var|rs_allocation <var.name>;\n}>"
			+ "int gInputDataXSize;\n"
			+ "int gInputDataYSize;\n"
			+ "<externalVariables:{var|<var.variableType> <var.variableName>;\n}>"
			+ "\n<functionSignature>\n {\n"
			+ "\t<userFunctionVarType> <userFunctionVarName>;\n"
			+ "\tfor(int x = 0; x <less> gInputDataXSize; ++x) {\n"
			+ "\t\tfor(int y = 0; y <less> gInputDataYSize; ++y) {\n"
			+ "\t\t\t<userFunctionVarName> = rsGetElementAt_<userFunctionVarType>(<inputData>, x, y);\n"
			+ "<userCode>\n"
			+ "\t\t}\n"
			+ "\t}\n"
			+ "\t<externalVariables:{var|rsSetElementAt_<var.variableType>(<var.outVariableName>, <var.variableName>, 0);\n}>"
			+ "}";
	private static final String templateIteratorParallelCall = "<externalVariables:{var|<var.functionName>.set_<var.gVariableName>(<var.variableName>);\n}>"
			+ "<functionName>.forEach_root(<variable>, <variable>);\n";
	private static final String templateIteratorSequentialCall = "<externalVariables:{var|<var.type>[] <var.arrName> = new <var.type>[1];\n"
			+ "Allocation <var.allName> = Allocation.createSized(mRS, Element.F32(mRS), 1);\n"
			+ "<functionObjectName>.set_<var.gName>(<var.name>);\n"
			+ "<functionObjectName>.set_gOutput_<var.name>(<var.allName>);\n}>"
			+ "<functionObjectName>.set_gInput_<inputData>(<inputData>);\n"
			+ "<functionObjectName>.set_gInputDataXSize(<inputData>.getType().getX());\n"
			+ "<functionObjectName>.set_gInputDataYSize(<inputData>.getType().getY());\n"
			+ "<functionObjectName>.invoke_<functionName>();\n"
			+ "<externalVariables:{var|<var.allName>.copyTo(<var.arrName>);\n"
			+ "<var.name> = <var.arrName>[0];\n}>";

	private static final String templateAllocationOutputDataHDRImage = "Bitmap.createBitmap(<inputAllocation>.getType().getX(), <inputAllocation>.getType().getY(), Bitmap.Config.ARGB_8888);\n";
	private static final String createAllocationFunctionBitmapImage = "float3 __attribute__((kernel)) root(uchar4 in, uint32_t x, uint32_t y) {"
			+ "\n\tfloat3 out;"
			+ "\n\tout.s0 = ((float) in.r) / 255.0f;"
			+ "\n\tout.s1 = ((float) in.g) / 255.0f;"
			+ "\n\tout.s2 = ((float) in.b) / 255.0f;"
			+ "\n\treturn out;"
			+ "\n}";
	private static final String createAllocationFunctionHDRImage = "float4 __attribute__((kernel)) root(uchar4 in, uint32_t x, uint32_t y) {"
			+ "\n\tfloat4 out;"
			+ "\n\tfloat f;"
			+ "\n\tif(in.s3 != 0) {"
			+ "\n\t\tf = ldexp(1.0f, (in.s3 & 0xFF) - (128 + 8));"
			+ "\n\t\tout.s0 = (in.s0 & 0xFF) * f;"
			+ "\n\t\tout.s1 = (in.s1 & 0xFF) * f;"
			+ "\n\t\tout.s2 = (in.s2 & 0xFF) * f;"
			+ "\n\t} else {"
			+ "\n\t\tout.s0 = 0.0f;"
			+ "\n\t\tout.s1 = 0.0f;"
			+ "\n\t\tout.s2 = 0.0f;" + "\n\t}" + "\n\treturn out;" + "\n}";

	public RenderScriptRuntimeDefinition(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInitializationString(String className,
			int firstFunctionNumber, int functionCount) {
		StringBuilder init = new StringBuilder();
		init.append("\tRenderScript mRS;\n");
		ST st1 = new ST(templateFunctions);
		ST st2 = new ST(templateConstructor);
		st2.add("ClassName", className);
		for (int i = firstFunctionNumber; i < firstFunctionNumber
				+ functionCount; i++) {
			st1.add("functions", this.getFunctionName(i));
			st2.add("functions", this.getFunctionName(i));
		}
		init.append(st1.render() + "\n ");
		init.append(st2.render());
		return init.toString();
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
			ST st = new ST(templateCreateAllocationBitmapImage);
			st.add("dataTypeInputObject", dataTypeInputObject);
			st.add("inputObject", inputObject);
			st.add("outputObject", outputObject);
			st.add("param", inputBind.getParameters()[0]);
			st.add("functionName",
					this.getFunctionName(inputBind.sequentialNumber));
			ret = st.render();
		} else if (userLibraryClass instanceof HDRImage) {
			String resourceData = this.getPrefix() + inputBind.getVariable()
					+ "ResourceData";
			StringBuilder params = new StringBuilder();
			for (int i = 0; i < inputBind.getParameters().length; i++) {
				params.append(inputBind.getParameters()[i]);
				if (i != (inputBind.getParameters().length - 1))
					params.append(",");
			}
			ST st = new ST(templateCreateAllocationHDRImage);
			st.add("resourceData", resourceData);
			st.add("params", params.toString());
			st.add("dataTypeInputObject", dataTypeInputObject);
			st.add("dataTypeOutputObject", dataTypeOutputObject);
			st.add("inputObject", inputObject);
			st.add("outputObject", outputObject);
			st.add("functionName",
					this.getFunctionName(inputBind.sequentialNumber));
			ret = st.render();
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
		String ret = "";
		if (inputBind.getVariable().typeName.equals(BitmapImage.getName())) {
			ret = createAllocationFunctionBitmapImage;
		} else if (inputBind.getVariable().typeName.equals(HDRImage.getName())) {
			ret = createAllocationFunctionHDRImage;
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAllocationData(OutputBind outputBind) {
		String inputObject = this.getVariableInName(outputBind.getVariable());
		String outputObject = this.getVariableOutName(outputBind.getVariable());
		StringBuilder ret = new StringBuilder();
		if (outputBind.getVariable().typeName.equals(HDRImage.getName())) {
			ST st = new ST(templateAllocationOutputDataHDRImage);
			st.add("inputAllocation", inputObject);
			ret.append(st.render());
		}
		ret.append(this.getFunctionName(outputBind.sequentialNumber)
				+ "_script.forEach_root(" + outputObject + ", " + inputObject
				+ ");\n" + inputObject + ".copyTo("
				+ outputBind.getDestinationObject().name + ");\n");
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAllocationDataFunction(OutputBind outputBind) {
		String ret = "";
		String typeName = outputBind.getVariable().typeName;
		if (typeName.equals(BitmapImage.getName())
				|| typeName.equals(HDRImage.getName())) {
			String varType;
			if (typeName.equals(HDRImage.getName())) {
				varType = "float4";
			} else {
				varType = "float3";
			}
			ST st = new ST(templateAllocationDataFunctionBitmapHDRImage);
			st.add("varType", varType);
			ret = st.render();
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIterator(Iterator iterator) {
		String functionName = this.getFunctionName(iterator.sequentialNumber);
		String functionObjectName = functionName + "_script";
		String ret;
		if (iterator.getType() == IteratorType.Parallel) {
			ST st = new ST(templateIteratorParallelCall);
			st.add("functionName", functionObjectName);
			st.add("variable", this.getVariableOutName(iterator.getVariable()));
			st.add("externalVariables", null);
			if (!iterator.getExternalVariables().isEmpty()) {
				for (Variable variable : iterator.getExternalVariables()) {
					String gVariable = this.getGlobalVariableName(variable);
					st.addAggr(
							"externalVariables.{functionName, gVariableName, variableName}",
							functionObjectName, gVariable, variable.name);
				}
			}
			ret = st.render();
		} else {
			ST st = new ST(templateIteratorSequentialCall);
			st.add("functionName", functionName);
			st.add("functionObjectName", functionObjectName);
			st.add("inputData", this.getVariableOutName(iterator.getVariable()));
			for (Variable variable : iterator.getExternalVariables()) {
				String gName = this.getGlobalVariableName(variable);
				String allocationName = gName + "Allocation";
				st.addAggr(
						"externalVariables.{type, arrName, name, gName, allName}",
						variable.typeName, this.getPrefix() + variable.name,
						variable.name, this.getGlobalVariableName(variable),
						allocationName);
			}
			ret = st.render();
		}
		return ret;
	}

	/**
	 * Create a global variable name for the given variable following
	 * RenderScript standards. In RenderScript, global variables must be
	 * prefixed with "g" followed by an upper case letter, so "max" becomes
	 * "gMax"
	 */
	private String getGlobalVariableName(Variable variable) {
		return "g" + variable.name.substring(0, 1).toUpperCase()
				+ variable.name.substring(1, variable.name.length());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateIteratorCode(Iterator iterator) {
		String ret;
		String code2Translate = iterator.getUserFunctionData().Code.trim();
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		if (iterator.getType() == IteratorType.Parallel) {
			String returnString = "return " + userFunctionVariable.name + ";";
			code2Translate = code2Translate + "\n" + returnString + "\n}";
			// Insert external variables as global variables
			StringBuffer externalVariables = new StringBuffer();
			for (Variable variable : iterator.getExternalVariables()) {
				String gVariableName = this.getGlobalVariableName(variable);
				externalVariables.append(variable.typeName + " "
						+ gVariableName + ";\n");
				code2Translate = code2Translate.replaceAll(variable.name,
						gVariableName);
			}
			externalVariables.append("\n");
			ret = externalVariables.toString()
					+ this.getIteratorFunctionSignature(iterator)
					+ this.translateVariable(userFunctionVariable,
							this.cCodeTranslator.translate(code2Translate));
		} else {
			// Remove the first curly brace
			code2Translate = code2Translate.substring(
					code2Translate.indexOf("{") + 1, code2Translate.length());
			ST st = new ST(templateIteratorSequentialFunction);
			String inputData = this.getVariableOutName(iterator.getVariable());
			st.add("less", "<");
			st.add("inputData", "gInput_" + inputData);
			st.add("functionSignature",
					this.getIteratorFunctionSignature(iterator));
			st.add("userFunctionVarName", userFunctionVariable.name);
			st.add("userFunctionVarType",
					this.translateType(userFunctionVariable.typeName));
			String cCode = this.translateVariable(userFunctionVariable,
					this.cCodeTranslator.translate(code2Translate)).trim();
			for (Variable variable : iterator.getExternalVariables()) {
				String gNameOut = "gOutput_" + variable;
				st.addAggr("outVariable.{name}", gNameOut);
				String gNameVar = this.getGlobalVariableName(variable);
				st.addAggr(
						"externalVariables.{ variableType, outVariableName, variableName }",
						variable.typeName, gNameOut, gNameVar);
				cCode = cCode.replaceAll(variable.name, gNameVar);
			}
			st.add("userCode", cCode);
			ret = st.render();
		}
		return ret;
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
		} else if (variable.typeName.equals(Pixel.getName())) {
			translatedCode = this.translatePixelVariable(variable, code);
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
		} else if (typeName.equals(Pixel.getName())) {
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

	private String translatePixelVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".x", "x");
		ret = ret.replaceAll(variable.name + ".y", "y");
		ret = ret
				.replaceAll(variable.name + ".rgba.red", variable.name + ".s0");
		ret = ret.replaceAll(variable.name + ".rgba.green", variable.name
				+ ".s1");
		ret = ret.replaceAll(variable.name + ".rgba.blue", variable.name
				+ ".s2");
		ret = ret.replaceAll(variable.name + ".rgba.alpha", variable.name
				+ ".s3");
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
		if (iterator.getVariable().typeName.equals(BitmapImage.getName())
				|| iterator.getVariable().typeName.equals(HDRImage.getName())) {
			if (iterator.getType() == IteratorType.Parallel) {
				ST st = new ST(templateIteratorParallelFunctionSignature);
				st.add("parameterTypeTranslated", parameterTypeTranslated);
				st.add("userFunctionName",
						iterator.getUserFunctionData().variableArgument.name);
				functionSignature = st.render();
			} else {
				ST st = new ST(templateIteratorSequentialFunctionSignature);
				st.add("functionName",
						this.getFunctionName(iterator.sequentialNumber));
				functionSignature = st.render();
			}
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
		return "#pragma version(1)\n#pragma rs java_package_name("
				+ packageName + ")\n";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exportInternalLibrary(String packageName,
			String destinationFolder) throws IOException {
		this.exportResource("Common", destinationFolder);
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(MethodCall methodCall) {
		String ret = "";
		if (methodCall.variable.typeName.equals(BitmapImage.getName())
				|| methodCall.variable.typeName.equals(HDRImage.getName())) {
			if (methodCall.methodName.equals(BitmapImage.getInstance()
					.getWidthMethodName())) {
				ret = this.getVariableInName(methodCall.variable)
						+ ".getType().getX()";
			} else if (methodCall.methodName.equals(BitmapImage.getInstance()
					.getHeightMethodName())) {
				ret = this.getVariableInName(methodCall.variable)
						+ ".getType().getY()";
			}
		} else {
			SimpleLogger.error("");
		}
		return ret;
	}
}
