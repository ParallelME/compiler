/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.renderscript;

import java.util.HashMap;
import java.util.Map;

import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.BaseTranslator;
import org.parallelme.compiler.userlibrary.classes.Array;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Base class for RenderScript translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSTranslator extends BaseTranslator {
	private static final String templateIteratorParallelCall = "<externalVariables:{var|<var.kernelName>.set_<var.gVariableName>(<var.variableName>);\n}>"
			+ "<kernelName>.forEach_<functionName>(<allocationName>, <allocationName>);";
	private static final String templateIteratorSequentialCall = "<externalVariables:{var|"
			+ "Allocation <var.allName> = Allocation.createSized($mRS, Element.<var.elementType>($mRS), 1);\n"
			+ "<kernelName>.set_<var.gName>(<var.name>[0]);\n"
			+ "<kernelName>.set_<var.outputData>(<var.allName>);\n}>"
			+ "<kernelName>.set_<inputData>(<allocationName>);\n"
			+ "<inputSize:{var|<kernelName>.set_<var.name>(<allocationName>.getType().get<var.XYZ>());\n}>"
			+ "<kernelName>.invoke_<functionName>();\n"
			+ "<externalVariables:{var|<var.allName>.copyTo(<var.name>);}>";
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

	private static final String templateForLoop = "for (int <varName> = 0; <varName> \\< <varMaxVal>; <varName>++) {\n\t<body>}\n";
	private static final String templateForLoopSequentialBody = "<userFunctionVarName> = rsGetElementAt_<userFunctionVarType>(<inputData>, x<param:{var|, <var.name>}>);\n"
			+ "<userCode>\n"
			+ "rsSetElementAt_<userFunctionVarType>(<inputData>, <userFunctionVarName>, x<param:{var|, <var.name>}>);\n";
	private static final String templateIteratorParallelFunctionSignature = "<parameterTypeTranslated> __attribute__((kernel)) <userFunctionName>(<parameterTypeTranslated> <parameterName>, uint32_t x<params:{var|, <var.type> <var.name>}>)";
	private static final String templateIteratorSequentialFunctionSignature = "void <functionName>()";

	// Keeps a key-value map of equivalent types from Java to RenderScript
	// allocation.
	private static Map<String, String> java2RSAllocationTypes = null;
	protected CTranslator cCodeTranslator;

	public RSTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
		if (java2RSAllocationTypes == null)
			this.initJava2RSAllocationTypes();
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
	public String translateIteratorCall(String className, Iterator iterator) {
		String functionName = this.commonDefinitions.getIteratorName(iterator);
		String kernelName = this.commonDefinitions.getKernelName(className);
		ST st;
		if (iterator.getType() == IteratorType.Parallel) {
			st = new ST(templateIteratorParallelCall);
			st.add("kernelName", kernelName);
			st.add("functionName", functionName);
			st.add("externalVariables", null);
			if (iterator.getExternalVariables().length > 0) {
				for (Variable variable : iterator.getExternalVariables()) {
					String gVariable = this.getGlobalVariableName(
							variable.name, iterator);
					st.addAggr(
							"externalVariables.{kernelName, gVariableName, variableName}",
							kernelName, gVariable, variable.name);
				}
			}
		} else {
			String inputData = this
					.getGlobalVariableName(
							"input"
									+ this.upperCaseFirstLetter(iterator.variable.name),
							iterator);
			String iteratorName = this.upperCaseFirstLetter(functionName);
			st = new ST(templateIteratorSequentialCall);
			st.add("kernelName", kernelName);
			st.add("functionName", functionName);
			st.add("inputData", inputData);
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
						"externalVariables.{name, gName, allName, elementType, outputData}",
						variable.name,
						this.getGlobalVariableName(variable.name, iterator),
						allocationName,
						java2RSAllocationTypes.get(variable.typeName),
						outputData);
			}
			st.addAggr("inputSize.{name, XYZ}", "gInputXSize" + iteratorName,
					"X");
			if (iterator.variable.typeName.equals(BitmapImage.getName())
					|| iterator.variable.typeName.equals(HDRImage.getName())) {
				st.addAggr("inputSize.{name, XYZ}", "gInputYSize"
						+ iteratorName, "Y");
			}
		}
		if (iterator.variable.typeName.equals(Array.getName()))
			st.add("allocationName",
					this.commonDefinitions.getVariableInName(iterator.variable));
		else
			st.add("allocationName", this.commonDefinitions
					.getVariableOutName(iterator.variable));
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelIterator(Iterator iterator) {
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		String code2Translate = iterator.getUserFunctionData().Code.trim();
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateSequentialIterator(Iterator iterator) {
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		String code2Translate = iterator.getUserFunctionData().Code.trim();
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
		// Remove the first curly brace
		code2Translate = code2Translate.substring(
				code2Translate.indexOf("{") + 1, code2Translate.length());
		ST st = new ST(templateIteratorSequentialFunction);
		String variableName = this.upperCaseFirstLetter(iterator.variable.name);
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
		stFor.add("varMaxVal", "gInputXSize" + iteratorName);
		ST stForBody = new ST(templateForLoopSequentialBody);
		stForBody.add("inputData", gNameIn);
		stForBody.add("userFunctionVarName", userFunctionVariable.name);
		stForBody.add("userFunctionVarType", userFunctionVarType);
		stForBody.add("userCode", cCode);
		stForBody.add("param", null);
		// BitmapImage and HDRImage types contains two for loops
		if (iterator.variable.typeName.equals(BitmapImage.getName())
				|| iterator.variable.typeName.equals(HDRImage.getName())) {
			stForBody.addAggr("param.{name}", "y");
			ST stFor2 = new ST(templateForLoop);
			stFor2.add("varName", "y");
			stFor2.add("varMaxVal", "gInputYSize" + iteratorName);
			stFor2.add("body", stForBody.render());
			stFor.add("body", stFor2.render());
		} else {
			// Array types
			stFor.add("body", stForBody.render());
		}
		st.add("forLoop", stFor.render());
		return st.render();
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
			if (iterator.variable.typeName.equals(BitmapImage.getName())
					|| iterator.variable.typeName.equals(HDRImage.getName())) {
				st.addAggr("params.{type, name}", "uint32_t", "y");
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
}
