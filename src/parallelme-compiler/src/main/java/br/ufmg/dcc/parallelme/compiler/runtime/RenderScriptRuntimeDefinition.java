/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.stringtemplate.v4.ST;

import br.ufmg.dcc.parallelme.compiler.SimpleLogger;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Iterator.IteratorType;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClass;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.*;
import br.ufmg.dcc.parallelme.compiler.util.FileWriter;

/**
 * Definitions for RenderScript runtime.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class RenderScriptRuntimeDefinition extends RuntimeDefinitionImpl {
	private static final String templateRSFile = "<introductoryMsg>\n<header>\n<functions:{functionName|\n\n<functionName>}>";
	private static final String templateKernels = "\t<kernels:{kernelName|ScriptC_<className> <kernelName>;\n}>";
	private static final String templateConstructor = "\tpublic <className>(RenderScript $mRS) {\n\t\tthis.$mRS = $mRS;\n\t\t<kernels:{kernelName|this.<kernelName> = new ScriptC_<className>($mRS);\n}>\t}\n";
	private static final String templateCreateAllocationArray = "<inputObject> = Allocation.createSized($mRS, Element.<elementType>, <allocationLength>);\n"
			+ "<inputObject>.copyFrom(<inputArray>);\n"
			+ "<outputObject> = Allocation.createSized($mRS, Element.<elementType>, <allocationLength>);";
	private static final String templateCreateAllocationBitmapImage = "Type <dataTypeInputObject>;\n"
			+ "<inputObject> = Allocation.createFromBitmap($mRS, <param>, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);\n"
			+ "<dataTypeInputObject> = new Type.Builder($mRS, Element.F32_3($mRS))\n"
			+ "\t.setX(<inputObject>.getType().getX())\n"
			+ "\t.setY(<inputObject>.getType().getY())\n"
			+ "\t.create();\n"
			+ "<outputObject> = Allocation.createTyped($mRS, <dataTypeInputObject>);\n"
			+ "<kernelName>.forEach_toFloat(<inputObject>, <outputObject>);";
	private static final String templateCreateAllocationHDRImage = "RGBE.ResourceData <resourceData> = RGBE.loadFromResource(<params>);\n"
			+ "Type <dataTypeInputObject> = new Type.Builder($mRS, Element.RGBA_8888($mRS))\n"
			+ "\t.setX(<resourceData>.width)\n"
			+ "\t.setY(<resourceData>.height)\n"
			+ "\t.create();\n"
			+ "Type <dataTypeOutputObject> = new Type.Builder($mRS, Element.F32_4($mRS))\n"
			+ "\t.setX(<resourceData>.width)\n"
			+ "\t.setY(<resourceData>.height)\n"
			+ "\t.create();\n"
			+ "<inputObject> = Allocation.createTyped($mRS, <dataTypeInputObject>, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);\n"
			+ "<outputObject> = Allocation.createTyped($mRS, <dataTypeOutputObject>);\n"
			+ "<inputObject>.copyFrom(<resourceData>.data);\n"
			+ "<kernelName>.forEach_toFloat(<inputObject>, <outputObject>);";
	private static final String templateAllocationDataFunctionBitmapHDRImage = "\nuchar4 __attribute__((kernel)) toBitmap(<varType>"
			+ " in, uint32_t x, uint32_t y) {"
			+ "\n\tuchar4 out;"
			+ "\n\tout.r = (uchar) (in.s0);"
			+ "\n\tout.g = (uchar) (in.s1);"
			+ "\n\tout.b = (uchar) (in.s2);"
			+ "\n\tout.a = <alphaValue>;"
			+ "\n\treturn out;\n}";
	private static final String templateIteratorParallelFunctionSignature = "<parameterTypeTranslated> __attribute__((kernel)) <userFunctionName>(<parameterTypeTranslated> <parameterName>, uint32_t x<params:{var|, <var.type> <var.name>}>)";
	private static final String templateIteratorSequentialFunctionSignature = "void <functionName>()";
	private static final String templateIteratorParallelCall = "<externalVariables:{var|<var.kernelName>.set_<var.gVariableName>(<var.variableName>);\n}>"
			+ "<kernelName>.forEach_<functionName>(<variable>, <variable>);\n";
	private static final String templateIteratorSequentialCall = "<externalVariables:{var|<var.type>[] <var.arrName> = new <var.type>[1];\n"
			+ "Allocation <var.allName> = Allocation.createSized($mRS, Element.<var.elementType>($mRS), 1);\n"
			+ "<kernelName>.set_<var.gName>(<var.name>);\n"
			+ "<kernelName>.set_<var.outputData>(<var.allName>);\n}>"
			+ "<kernelName>.set_<inputData>(<inputDataVar>);\n"
			+ "<inputSize:{var|<kernelName>.set_<var.name>(<inputDataVar>.getType().get<var.XYZ>());\n}>"
			+ "<kernelName>.invoke_<functionName>();\n"
			+ "<externalVariables:{var|<var.allName>.copyTo(<var.arrName>);\n"
			+ "<var.name> = <var.arrName>[0];\n}>";
	private static final String templateIteratorSequentialFunction = "rs_allocation <inputData>;\n"
			+ "<outVariable:{var|rs_allocation <var.name>;\n}>"
			+ "int gInputXSize<iteratorName>;\n"
			+ "int gInputYSize<iteratorName>;\n"
			+ "<externalVariables:{var|<var.variableType> <var.variableName>;\n}>"
			+ "\n<functionSignature>\n {\n"
			+ "\t<userFunctionVarType> <userFunctionVarName>;\n"
			+ "\t<forLoop>"
			+ "\t<externalVariables:{var|rsSetElementAt_<var.variableType>(<var.outVariableName>, <var.variableName>, 0);\n}>"
			+ "}";
	private static final String templateAllocationOutputCreateBitmap = "<destinationObject> = Bitmap.createBitmap(<inputAllocation>.getType().getX(), <inputAllocation>.getType().getY(), Bitmap.Config.ARGB_8888);\n";
	private static final String templateAllocationOutputBitmapHDRImage = "<kernelName>.forEach_toBitmap(<outputObject>, <inputObject>);\n"
			+ "<inputObject>.copyTo(<destinationObject>);\n";
	private static final String templateAllocationOutputCreateArray = "<type> <name> = (<type>) java.lang.reflect.Array.newInstance(<baseType>.class, <inputAllocation>.getType().getX());\n";
	private static final String templateAllocationOutputArray = "<inputObject>.copyTo(<destinationObject>);\n";

	private static final String templateForLoop = "for (int <varName> = 0; <varName> <less> <varMaxVal>; <varName>++) {\n\t<body>}\n";
	private static final String templateForLoopSequentialBody = "<userFunctionVarName> = rsGetElementAt_<userFunctionVarType>(<inputData>, x<param:{var|, <var.name>}>);\n"
			+ "<userCode>\n"
			+ "rsSetElementAt_<userFunctionVarType>(<inputData>, <userFunctionVarName>, x<param:{var|, <var.name>}>);\n";
	private static final String templateCreateAllocationFunctionImage = "\nfloat3 __attribute__((kernel)) toFloat(uchar4 in, uint32_t x, uint32_t y) {"
			+ "\n\tfloat3 out;"
			+ "\n\tout.s0 = (float) in.r;"
			+ "\n\tout.s1 = (float) in.g;"
			+ "\n\tout.s2 = (float) in.b;"
			+ "\n\treturn out;" + "\n}";
	// Keeps a key-value map of equivalent types from ParallelME to RenderScript
	// allocation.
	private static Map<String, String> parallelME2RSAllocationTypes = null;
	// Keeps a key-value map of equivalent types from Java to RenderScript
	// allocation.
	private static Map<String, String> java2RSAllocationTypes = null;

	public RenderScriptRuntimeDefinition(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		super(cCodeTranslator, outputDestinationFolder);
		this.initParallelME2RSAllocationTypes();
		this.initJava2RSAllocationTypes();
	}

	private void initParallelME2RSAllocationTypes() {
		parallelME2RSAllocationTypes = new HashMap<>();
		parallelME2RSAllocationTypes.put(Int16.getName(), "I16");
		parallelME2RSAllocationTypes.put(Int32.getName(), "I32");
		parallelME2RSAllocationTypes.put(Float32.getName(), "F32");
	}

	private void initJava2RSAllocationTypes() {
		java2RSAllocationTypes = new HashMap<>();
		java2RSAllocationTypes.put("short", "I16");
		java2RSAllocationTypes.put("int", "I32");
		java2RSAllocationTypes.put("float", "F32");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInitializationString(String packageName, String className,
			List<InputBind> inputBinds, List<Iterator> iterators,
			List<OutputBind> outputBinds) {
		StringBuilder init = new StringBuilder();
		init.append("\tRenderScript $mRS;\n");
		ST st1 = new ST(templateKernels);
		ST st2 = new ST(templateConstructor);
		st1.add("className", className);
		st2.add("className", className);
		String kernelName = this.commonDefinitions.getKernelName(className);
		st1.add("kernels", kernelName);
		st2.add("kernels", kernelName);
		init.append(st1.render() + "\n ");
		init.append(st2.render());
		return init.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImports(List<UserLibraryData> iteratorsAndBinds) {
		StringBuffer ret = new StringBuffer();
		ret.append("import android.support.v8.renderscript.*;\n");
		ret.append(this.getUserLibraryImports(iteratorsAndBinds));
		ret.append("\n");
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String createAllocation(String className, InputBind inputBind) {
		String ret = "";
		String inputObject = this.commonDefinitions.getVariableInName(inputBind
				.getVariable());
		String outputObject = this.commonDefinitions
				.getVariableOutName(inputBind.getVariable());
		String dataTypeInputObject = this.commonDefinitions.getPrefix()
				+ inputBind.getVariable() + "InDataType";
		String dataTypeOutputObject = this.commonDefinitions.getPrefix()
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
			st.add("kernelName",
					this.commonDefinitions.getKernelName(className));
			ret = st.render();
		} else if (userLibraryClass instanceof HDRImage) {
			String resourceData = this.commonDefinitions.getPrefix()
					+ inputBind.getVariable() + "ResourceData";
			ST st = new ST(templateCreateAllocationHDRImage);
			st.add("resourceData", resourceData);
			st.add("params", this.commonDefinitions
					.toCommaSeparatedString(inputBind.getParameters()));
			st.add("dataTypeInputObject", dataTypeInputObject);
			st.add("dataTypeOutputObject", dataTypeOutputObject);
			st.add("inputObject", inputObject);
			st.add("outputObject", outputObject);
			st.add("kernelName",
					this.commonDefinitions.getKernelName(className));
			ret = st.render();
		} else if (userLibraryClass instanceof Array) {
			ST st = new ST(templateCreateAllocationArray);
			// TODO Check if parameters array ahas size 3, otherwise throw an
			// exception and abort translation.
			st.add("inputArray", inputBind.getParameters()[0]);
			st.add("allocationLength", inputBind.getParameters()[2]);
			st.add("inputObject", inputObject);
			st.add("outputObject", outputObject);
			st.add("elementType", parallelME2RSAllocationTypes.get(inputBind
					.getVariable().typeParameterName));
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
		String inputObject = this.commonDefinitions.getVariableInName(inputBind
				.getVariable());
		String outputObject = this.commonDefinitions
				.getVariableOutName(inputBind.getVariable());
		ret = "Allocation " + inputObject + ", " + outputObject + ";\n";
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAllocationData(String className, OutputBind outputBind) {
		StringBuilder ret = new StringBuilder();
		Variable variable = outputBind.getVariable();
		String inputObject = this.commonDefinitions.getVariableInName(variable);
		String outputObject = this.commonDefinitions
				.getVariableOutName(variable);
		String destinationObject = outputBind.destinationObject.name;
		if (variable.typeName.equals(BitmapImage.getName())
				|| variable.typeName.equals(HDRImage.getName())) {
			// If it is an object declaration, must declare the destination
			// object type and name.
			if (outputBind.isObjectDeclaration) {
				ST st = new ST(templateAllocationOutputCreateBitmap);
				st.add("inputAllocation", inputObject);
				st.add("destinationObject",
						outputBind.destinationObject.typeName + " "
								+ destinationObject);
				ret.append(st.render());
			}
			ST st = new ST(templateAllocationOutputBitmapHDRImage);
			st.add("kernelName",
					this.commonDefinitions.getKernelName(className));
			st.add("outputObject", outputObject);
			st.add("inputObject", inputObject);
			st.add("destinationObject", destinationObject);
			ret.append(st.render());
		} else if (variable.typeName.equals(Array.getName())) {
			// If it is an object declaration, must declare the destination
			// object type and name.
			if (outputBind.isObjectDeclaration) {
				ST st = new ST(templateAllocationOutputCreateArray);
				st.add("type", outputBind.destinationObject.typeName);
				st.add("name", outputBind.destinationObject.name);
				String baseType = outputBind.destinationObject.typeName
						.replaceAll("\\[", "").replaceAll("\\]", "").trim();
				st.add("baseType", baseType);
				st.add("inputAllocation", inputObject);
				ret.append(st.render());
			}
			ST st = new ST(templateAllocationOutputArray);
			st.add("inputObject", inputObject);
			st.add("destinationObject", destinationObject);
			ret.append(st.render());
		}
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIteratorCall(String className, Iterator iterator) {
		String functionName = this.commonDefinitions.getIteratorName(iterator);
		String kernelName = this.commonDefinitions.getKernelName(className);
		String ret;
		if (iterator.getType() == IteratorType.Parallel) {
			ST st = new ST(templateIteratorParallelCall);
			st.add("kernelName", kernelName);
			st.add("functionName", functionName);
			st.add("variable", this.commonDefinitions
					.getVariableOutName(iterator.getVariable()));
			st.add("externalVariables", null);
			if (!iterator.getExternalVariables().isEmpty()) {
				for (Variable variable : iterator.getExternalVariables()) {
					String gVariable = this.getGlobalVariableName(
							variable.name, iterator);
					st.addAggr(
							"externalVariables.{kernelName, gVariableName, variableName}",
							kernelName, gVariable, variable.name);
				}
			}
			ret = st.render();
		} else {
			String inputData = this
					.getGlobalVariableName(
							"input"
									+ this.upperCaseFirstLetter(iterator
											.getVariable().name), iterator);
			String iteratorName = this.upperCaseFirstLetter(functionName);
			ST st = new ST(templateIteratorSequentialCall);
			st.add("kernelName", kernelName);
			st.add("functionName", functionName);
			st.add("inputData", inputData);
			st.add("inputDataVar", this.commonDefinitions
					.getVariableOutName(iterator.getVariable()));
			st.add("iteratorName", iteratorName);
			for (Variable variable : iterator.getExternalVariables()) {
				String gName = this.getGlobalVariableName(variable.name,
						iterator);
				String allocationName = this.commonDefinitions.getPrefix()
						+ gName + "_Allocation";
				String outputData = this.getGlobalVariableName(
						"output" + this.upperCaseFirstLetter(variable.name),
						iterator);
				st.addAggr(
						"externalVariables.{type, arrName, name, gName, allName, elementType, outputData}",
						variable.typeName, this.commonDefinitions.getPrefix()
								+ variable.name, variable.name,
						this.getGlobalVariableName(variable.name, iterator),
						allocationName,
						java2RSAllocationTypes.get(variable.typeName),
						outputData);
			}
			st.addAggr("inputSize.{name, XYZ}", "gInputXSize" + iteratorName,
					"X");
			if (iterator.getVariable().typeName.equals(BitmapImage.getName())
					|| iterator.getVariable().typeName.equals(HDRImage
							.getName())) {
				st.addAggr("inputSize.{name, XYZ}", "gInputYSize"
						+ iteratorName, "Y");
			}
			ret = st.render();
		}
		return ret;
	}

	/**
	 * Create a global variable name for the given variable following some
	 * standards. Global variables will be prefixed with "g" followed by an
	 * upper case letter and sufixed by the iterator name, so "max" from
	 * iterator 2 becomes "gMax_Iterator2"
	 */
	private String getGlobalVariableName(String variable, Iterator iterator) {
		String iteratorName = this.commonDefinitions.getIteratorName(iterator);
		String variableName = this.upperCaseFirstLetter(variable);
		return "g" + variableName + this.upperCaseFirstLetter(iteratorName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean translateIteratorsAndBinds(String packageName,
			String className, List<Iterator> iterators,
			List<InputBind> inputBinds, List<OutputBind> outputBinds) {
		// 1. Add file header
		ST st = new ST(templateRSFile);
		st.add("introductoryMsg", this.commonDefinitions.getHeaderComment());
		st.add("header", "#pragma version(1)\n#pragma rs java_package_name("
				+ packageName + ")");
		// 2. Translate input binds
		Set<String> inputBindTypes = new HashSet<String>();
		for (InputBind inputBind : inputBinds) {
			if (!inputBindTypes.contains(inputBind.getVariable().typeName)) {
				inputBindTypes.add(inputBind.getVariable().typeName);
				st.add("functions", this.translateInputBind(inputBind));
			}
		}
		// 3. Translate iterators
		for (Iterator iterator : iterators)
			st.add("functions", this.translateIterator(iterator));
		// 4. Translate outputbinds
		Set<String> outputBindTypes = new HashSet<String>();
		for (OutputBind outputBind : outputBinds) {
			if (!outputBindTypes.contains(outputBind.getVariable().typeName)) {
				outputBindTypes.add(outputBind.getVariable().typeName);
				st.add("functions", this.translateOutputBind(outputBind));
			}
		}
		// 5. Write translated file
		FileWriter.writeFile(className + ".rs", this.outputDestinationFolder,
				st.render());
		return true;
	}

	/**
	 * Translates a given iterator.
	 * 
	 * @param iterator
	 *            Object containing the necessary information to translate an
	 *            iterator.
	 * 
	 * @return A string with the translated function for the given iterator.
	 */
	private String translateIterator(Iterator iterator) {
		String ret;
		String code2Translate = iterator.getUserFunctionData().Code.trim();
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		// Translate parallel iterators
		if (iterator.getType() == IteratorType.Parallel) {
			ret = this.translateParallelIterator(iterator, code2Translate,
					userFunctionVariable);
		} else {
			ret = this.translateSequentialIterator(iterator, code2Translate,
					userFunctionVariable);
		}
		return ret;
	}

	private String translateParallelIterator(Iterator iterator,
			String code2Translate, Variable userFunctionVariable) {
		String ret;
		String returnString = "\treturn " + userFunctionVariable.name + ";";
		code2Translate = code2Translate + "\n" + returnString + "\n}";
		// Insert external variables as global variables
		StringBuffer externalVariables = new StringBuffer();
		for (Variable variable : iterator.getExternalVariables()) {
			String gVariableName = this.getGlobalVariableName(variable.name,
					iterator);
			externalVariables.append(variable.typeName + " " + gVariableName
					+ ";\n");
			code2Translate = this.replaceAndEscapePrefix(code2Translate,
					gVariableName, variable.name);
		}
		externalVariables.append("\n");
		ret = externalVariables.toString()
				+ this.getIteratorFunctionSignature(iterator)
				+ this.translateVariable(userFunctionVariable,
						this.cCodeTranslator.translate(code2Translate));
		return ret;
	}

	private String translateSequentialIterator(Iterator iterator,
			String code2Translate, Variable userFunctionVariable) {
		String ret;
		// Remove the first curly brace
		code2Translate = code2Translate.substring(
				code2Translate.indexOf("{") + 1, code2Translate.length());
		ST st = new ST(templateIteratorSequentialFunction);
		String variableName = this
				.upperCaseFirstLetter(iterator.getVariable().name);
		String gNameIn = this.getGlobalVariableName("input" + variableName,
				iterator);
		String iteratorName = this.upperCaseFirstLetter(this.commonDefinitions
				.getIteratorName(iterator));
		st.add("inputData", gNameIn);
		st.add("functionSignature", this.getIteratorFunctionSignature(iterator));
		st.add("iteratorName", iteratorName);
		String userFunctionVarType = this
				.translateType(userFunctionVariable.typeName);
		st.add("userFunctionVarName", userFunctionVariable.name);
		st.add("userFunctionVarType", userFunctionVarType);
		String cCode = this.translateVariable(userFunctionVariable,
				this.cCodeTranslator.translate(code2Translate)).trim();
		for (Variable variable : iterator.getExternalVariables()) {
			String gNameOut = this.getGlobalVariableName(
					"output" + this.upperCaseFirstLetter(variable.name),
					iterator);
			st.addAggr("outVariable.{name}", gNameOut);
			String gNameVar = this.getGlobalVariableName(variable.name,
					iterator);
			st.addAggr(
					"externalVariables.{ variableType, outVariableName, variableName }",
					variable.typeName, gNameOut, gNameVar);
			cCode = this.replaceAndEscapePrefix(cCode, gNameVar, variable.name);
		}
		ST stFor = new ST(templateForLoop);
		stFor.add("varName", "x");
		stFor.add("less", "<");
		stFor.add("varMaxVal", "gInputXSize" + iteratorName);
		ST stForBody = new ST(templateForLoopSequentialBody);
		stForBody.add("inputData", gNameIn);
		stForBody.add("userFunctionVarName", userFunctionVariable.name);
		stForBody.add("userFunctionVarType", userFunctionVarType);
		stForBody.add("userCode", cCode);
		stForBody.add("param", null);
		// BitmapImage and HDRImage types contains two for loops
		if (iterator.getVariable().typeName.equals(BitmapImage.getName())
				|| iterator.getVariable().typeName.equals(HDRImage.getName())) {
			stForBody.addAggr("param.{name}", "y");
			ST stFor2 = new ST(templateForLoop);
			stFor2.add("varName", "y");
			stFor2.add("less", "<");
			stFor2.add("varMaxVal", "gInputYSize" + iteratorName);
			stFor2.add("body", stForBody.render());
			stFor.add("body", stFor2.render());
		} else {
			// Array types
			stFor.add("body", stForBody.render());
		}
		st.add("forLoop", stFor.render());
		ret = st.render();
		return ret;
	}

	/**
	 * Change the first letter of the informed string to upper case.
	 */
	private String upperCaseFirstLetter(String string) {
		return string.substring(0, 1).toUpperCase()
				+ string.substring(1, string.length());
	}

	/**
	 * Replace all instances of a given oldName by a newName, checking if this
	 * newName contains a $ sign, which is reserved in regex. In case a $
	 * exists, it will be escaped during replacement.
	 */
	private String replaceAndEscapePrefix(String string, String newName,
			String oldName) {
		if (newName.contains("$")) {
			int idx = newName.indexOf("$");
			newName = newName.substring(0, idx) + "\\$"
					+ newName.substring(idx + 1, newName.length());
			return string.replaceAll(oldName, newName);
		} else {
			return string.replaceAll(oldName, newName);
		}
	}

	/**
	 * Creates the code that is necessary to perform input binding.
	 * 
	 * @param inputBind
	 *            Object containing the necessary information to build an
	 *            allocation.
	 * 
	 * @return A string with the declaration for the new allocation.
	 */
	private String translateInputBind(InputBind inputBind) {
		String ret = "";
		if (inputBind.getVariable().typeName.equals(BitmapImage.getName())
				|| inputBind.getVariable().typeName.equals(HDRImage.getName())) {
			ret = templateCreateAllocationFunctionImage;
		}
		return ret;
	}

	/**
	 * Creates the code that is necessary to perform ouput binding.
	 * 
	 * @param outputBind
	 *            Object containing the necessary information to perform the
	 *            binding from an allocation to a destination object.
	 * 
	 * @return A string with the code to get the data from the allocation.
	 */
	private String translateOutputBind(OutputBind outputBind) {
		String ret = "";
		String typeName = outputBind.getVariable().typeName;
		if (typeName.equals(BitmapImage.getName())
				|| typeName.equals(HDRImage.getName())) {
			String varType;
			ST st = new ST(templateAllocationDataFunctionBitmapHDRImage);
			if (typeName.equals(HDRImage.getName())) {
				st.add("alphaValue", "(uchar) (in.s3)");
				varType = "float4";
			} else {
				st.add("alphaValue", "255");
				varType = "float3";
			}
			st.add("varType", varType);
			ret = st.render();
		}
		return ret;
	}

	/**
	 * Create the function signature for a given iterator.
	 * 
	 * @param iterator
	 *            Iterator that must be analyzed in order to create a function
	 *            signature.
	 * 
	 * @return Function signature.
	 */
	protected String getIteratorFunctionSignature(Iterator iterator) {
		String functionSignature = "";
		String parameterTypeTranslated = this.translateType(iterator
				.getUserFunctionData().variableArgument.typeName);
		if (iterator.getType() == IteratorType.Parallel) {
			ST st = new ST(templateIteratorParallelFunctionSignature);
			st.add("parameterTypeTranslated", parameterTypeTranslated);
			st.add("parameterName",
					iterator.getUserFunctionData().variableArgument.name);
			st.add("userFunctionName",
					this.commonDefinitions.getIteratorName(iterator));
			st.add("params", null);
			if (iterator.getVariable().typeName.equals(BitmapImage.getName())
					|| iterator.getVariable().typeName.equals(HDRImage
							.getName())) {
				st.addAggr("params.{type, name}", "uint32_t", "x");
			}
			functionSignature = st.render();
		} else {
			ST st = new ST(templateIteratorSequentialFunctionSignature);
			st.add("functionName",
					this.commonDefinitions.getIteratorName(iterator));
			functionSignature = st.render();
		}
		return functionSignature + " ";
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
	@Override
	public String translateMethodCall(MethodCall methodCall) {
		String ret = "";
		if (methodCall.variable.typeName.equals(BitmapImage.getName())
				|| methodCall.variable.typeName.equals(HDRImage.getName())) {
			if (methodCall.methodName.equals(BitmapImage.getInstance()
					.getWidthMethodName())) {
				ret = this.commonDefinitions
						.getVariableInName(methodCall.variable)
						+ ".getType().getX()";
			} else if (methodCall.methodName.equals(BitmapImage.getInstance()
					.getHeightMethodName())) {
				ret = this.commonDefinitions
						.getVariableInName(methodCall.variable)
						+ ".getType().getY()";
			}
		} else {
			SimpleLogger.error("");
		}
		return ret;
	}
}
