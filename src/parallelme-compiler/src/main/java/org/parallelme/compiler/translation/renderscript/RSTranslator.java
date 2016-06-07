/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import java.util.HashMap;
import java.util.Map;

import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.Array;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Base class for RenderScript translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSTranslator extends BaseUserLibraryTranslator {
	private static final String templateOperationParallelCall = "<externalVariables:{var|<var.kernelName>.set_<var.gVariableName>(<var.variableName>);\n}>"
			+ "<kernelName>.forEach_<functionName>(<allocationName>, <allocationName>);";
	private static final String templateOperationSequentialCall = "<externalVariables:{var|"
			+ "Allocation <var.allName> = Allocation.createSized(PM_mRS, Element.<var.elementType>(PM_mRS), 1);\n"
			+ "<kernelName>.set_<var.gName>(<var.name>[0]);\n"
			+ "<kernelName>.set_<var.outputData>(<var.allName>);\n}>"
			+ "<kernelName>.set_<inputData>(<allocationName>);\n"
			+ "<inputSize:{var|<kernelName>.set_<var.name>(<allocationName>.getType().get<var.XYZ>());\n}>"
			+ "<kernelName>.invoke_<functionName>();\n"
			+ "<externalVariables:{var|<var.allName>.copyTo(<var.name>);}>";
	private static final String templateOperationSequentialFunction = "rs_allocation <inputData>;\n"
			+ "<outVariable:{var|rs_allocation <var.name>;\n}>"
			+ "int PM_gInputXSize<operationName>;\n"
			+ "int PM_gInputYSize<operationName>;\n"
			+ "<externalVariables:{var|<var.variableType> <var.variableName>;\n}>"
			+ "\n<functionSignature>\n {\n"
			+ "\t<userFunctionVarType> <userFunctionVarName>;\n"
			+ "\t<forLoop>"
			+ "\t<externalVariables:{var|rsSetElementAt_<var.variableType>(<var.outVariableName>, <var.variableName>, 0);\n}>"
			+ "}";

	private static final String templateForLoop = "for (int <varName> = 0; <varName> \\< <varMaxVal>; <varName>++) {\n\t<body>}\n";
	private static final String templateForLoopSequentialBody = "<userFunctionVarName> = rsGetElementAt_<userFunctionVarType>(<inputData>, PM_x<param:{var|, <var.name>}>);\n"
			+ "<userCode>\n"
			+ "rsSetElementAt_<userFunctionVarType>(<inputData>, <userFunctionVarName>, PM_x<param:{var|, <var.name>}>);\n";
	private static final String templateOperationParallelFunctionSignature = "<parameterTypeTranslated> __attribute__((kernel)) <userFunctionName>(<parameterTypeTranslated> <parameterName>, uint32_t x<params:{var|, <var.type> <var.name>}>)";
	private static final String templateOperationSequentialFunctionSignature = "void <functionName>()";

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
	public String translateOperationCall(String className, Operation operation) {
		String functionName = this.commonDefinitions
				.getOperationName(operation);
		String kernelName = this.commonDefinitions.getKernelName(className);
		ST st;
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			st = new ST(templateOperationParallelCall);
			st.add("kernelName", kernelName);
			st.add("functionName", functionName);
			st.add("externalVariables", null);
			if (!operation.getExternalVariables().isEmpty()) {
				for (Variable variable : operation.getExternalVariables()) {
					String gVariable = this.getGlobalVariableName(
							variable.name, operation);
					st.addAggr(
							"externalVariables.{kernelName, gVariableName, variableName}",
							kernelName, gVariable, variable.name);
				}
			}
		} else {
			String inputData = this
					.getGlobalVariableName(
							"input"
									+ this.upperCaseFirstLetter(operation.variable.name),
							operation);
			String operationName = this.upperCaseFirstLetter(functionName);
			st = new ST(templateOperationSequentialCall);
			st.add("kernelName", kernelName);
			st.add("functionName", functionName);
			st.add("inputData", inputData);
			for (Variable variable : operation.getExternalVariables()) {
				String gName = this.getGlobalVariableName(variable.name,
						operation);
				String allocationName = gName + "_Allocation";
				String outputData = this.getGlobalVariableName(
						"output" + this.upperCaseFirstLetter(variable.name),
						operation);
				st.addAggr(
						"externalVariables.{name, gName, allName, elementType, outputData}",
						variable.name,
						this.getGlobalVariableName(variable.name, operation),
						allocationName,
						java2RSAllocationTypes.get(variable.typeName),
						outputData);
			}
			String prefix = this.commonDefinitions.getPrefix();
			st.addAggr("inputSize.{name, XYZ}", prefix + "gInputXSize"
					+ operationName, "X");
			if (operation.variable.typeName.equals(BitmapImage.getInstance()
					.getClassName())
					|| operation.variable.typeName.equals(HDRImage
							.getInstance().getClassName())) {
				st.addAggr("inputSize.{name, XYZ}", prefix + "gInputYSize"
						+ operationName, "Y");
			}
		}
		if (operation.variable.typeName.equals(Array.getInstance()
				.getClassName()))
			st.add("allocationName", this.commonDefinitions
					.getVariableInName(operation.variable));
		else
			st.add("allocationName", this.commonDefinitions
					.getVariableOutName(operation.variable));
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelForeach(Operation operation) {
		Variable userFunctionVariable = operation.getUserFunctionData().arguments
				.get(0);
		String code2Translate = operation.getUserFunctionData().Code.trim();
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
		String ret;
		String returnString = "\treturn " + userFunctionVariable.name + ";";
		code2Translate = code2Translate + "\n" + returnString + "\n}";
		// Insert external variables as global variables
		StringBuffer externalVariables = new StringBuffer();
		for (Variable variable : operation.getExternalVariables()) {
			String gVariableName = this.getGlobalVariableName(variable.name,
					operation);
			externalVariables.append(variable.typeName + " " + gVariableName
					+ ";\n");
			code2Translate = code2Translate.replaceAll(variable.name,
					gVariableName);
		}
		externalVariables.append("\n");
		ret = externalVariables.toString()
				+ this.getOperationFunctionSignature(operation)
				+ this.translateVariable(userFunctionVariable,
						this.cCodeTranslator.translate(code2Translate));
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelReduce(Operation operation) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelReduceTile(Operation operation) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelReduceUserFunction(Operation operation) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateSequentialForeach(Operation operation) {
		Variable userFunctionVariable = operation.getUserFunctionData().arguments
				.get(0);
		String code2Translate = operation.getUserFunctionData().Code.trim();
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
		// Remove the first curly brace
		code2Translate = code2Translate.substring(
				code2Translate.indexOf("{") + 1, code2Translate.length());
		ST st = new ST(templateOperationSequentialFunction);
		String variableName = this
				.upperCaseFirstLetter(operation.variable.name);
		String gNameIn = this.getGlobalVariableName("input" + variableName,
				operation);
		String operationName = this.upperCaseFirstLetter(this.commonDefinitions
				.getOperationName(operation));
		st.add("inputData", gNameIn);
		st.add("functionSignature",
				this.getOperationFunctionSignature(operation));
		st.add("operationName", operationName);
		String userFunctionVarType = this.commonDefinitions
				.translateType(userFunctionVariable.typeName);
		st.add("userFunctionVarName", userFunctionVariable.name);
		st.add("userFunctionVarType", userFunctionVarType);
		String cCode = this.translateVariable(userFunctionVariable,
				this.cCodeTranslator.translate(code2Translate)).trim();
		for (Variable variable : operation.getExternalVariables()) {
			String gNameOut = this.getGlobalVariableName(
					"output" + this.upperCaseFirstLetter(variable.name),
					operation);
			st.addAggr("outVariable.{name}", gNameOut);
			String gNameVar = this.getGlobalVariableName(variable.name,
					operation);
			st.addAggr(
					"externalVariables.{ variableType, outVariableName, variableName }",
					variable.typeName, gNameOut, gNameVar);
			cCode = cCode.replaceAll(variable.name, gNameVar);
		}
		String prefix = this.commonDefinitions.getPrefix();
		ST stFor = new ST(templateForLoop);
		stFor.add("varName", prefix + "x");
		stFor.add("varMaxVal", prefix + "gInputXSize" + operationName);
		ST stForBody = new ST(templateForLoopSequentialBody);
		stForBody.add("inputData", gNameIn);
		stForBody.add("userFunctionVarName", userFunctionVariable.name);
		stForBody.add("userFunctionVarType", userFunctionVarType);
		stForBody.add("userCode", cCode);
		stForBody.add("param", null);
		// BitmapImage and HDRImage types contains two for loops
		if (operation.variable.typeName.equals(BitmapImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(HDRImage.getInstance()
						.getClassName())) {
			stForBody.addAggr("param.{name}", prefix + "y");
			ST stFor2 = new ST(templateForLoop);
			stFor2.add("varName", prefix + "y");
			stFor2.add("varMaxVal", prefix + "gInputYSize" + operationName);
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
	 * {@inheritDoc}
	 */
	@Override
	protected String translateSequentialReduce(Operation operation) {
		return "";
	}

	/**
	 * Create the function signature for a given operation.
	 * 
	 * @param operation
	 *            Operation that must be analyzed in order to create a function
	 *            signature.
	 * 
	 * @return Function signature.
	 */
	protected String getOperationFunctionSignature(Operation operation) {
		String functionSignature = "";
		String parameterTypeTranslated = this.commonDefinitions
				.translateType(operation.getUserFunctionData().arguments.get(0).typeName);
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			ST st = new ST(templateOperationParallelFunctionSignature);
			st.add("parameterTypeTranslated", parameterTypeTranslated);
			st.add("parameterName",
					operation.getUserFunctionData().arguments.get(0).name);
			st.add("userFunctionName",
					this.commonDefinitions.getOperationName(operation));
			st.add("params", null);
			if (operation.variable.typeName.equals(BitmapImage.getInstance()
					.getClassName())
					|| operation.variable.typeName.equals(HDRImage
							.getInstance().getClassName())) {
				st.addAggr("params.{type, name}", "uint32_t", "y");
			}
			functionSignature = st.render();
		} else {
			ST st = new ST(templateOperationSequentialFunctionSignature);
			st.add("functionName",
					this.commonDefinitions.getOperationName(operation));
			functionSignature = st.render();
		}
		return functionSignature + " ";
	}
}
