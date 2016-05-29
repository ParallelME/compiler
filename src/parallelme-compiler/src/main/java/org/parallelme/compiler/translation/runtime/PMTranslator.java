/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.Array;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Base class for ParallelME runtime translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMTranslator extends BaseUserLibraryTranslator {
	protected static final String templateCallJNIFunction = "<jniJavaClassName>.getInstance().<functionName>(<params:{var|<var.name>}; separator=\", \">)";
	private static final String templateKernelDecl = "__kernel void <functionName>(<params:{var|<var.type> <var.name>}; separator=\", \">)";
	private static final String templateForLoop = "for (int <varName> = 0; <varName> \\< <varMaxVal>; ++<varName>) {\n\t<body>}\n";
	private static final String templateOperationCall = ""
			+ "<destinationVariable:{var|<var.type> <var.name> = new <var.type>();\n"
			+ "<var.name>.value = }>"
			+ "<operationName>(<params:{var|<var.name>}; separator=\", \">);"
			+ "<destinationVariable:{var|\n\nreturn <var.name>;}>";
	private static final String templateOperationSequentialFunction = "<functionSignature>\n {\n"
			+ "\t<forLoop>"
			+ "\t<externalVariables:{var|*<var.outVariableName> = <var.variableName>;\n}>"
			+ "}";

	protected CTranslator cCodeTranslator;

	public PMTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelOperation(Operation operation) {
		Variable userFunctionVariable = operation.getUserFunctionData().arguments[0];
		String code2Translate = operation.getUserFunctionData().Code.trim();
		code2Translate = this.removeCurlyBraces(code2Translate);
		String ret;
		String gidDeclaration = String.format(
				"int PM_gid = get_global_id(0);\n" + "\t%s %s = %s[PM_gid];",
				this.commonDefinitions
						.translateType(userFunctionVariable.typeName),
				userFunctionVariable.name, this.getDataVariableName());
		String dataOutReturn = String.format("%s[PM_gid] = %s;\n",
				this.getDataVariableName(), userFunctionVariable.name);
		ret = String.format("%s {\n\t%s %s %s\n}", this
				.getOperationFunctionSignature(operation), gidDeclaration, this
				.translateVariable(userFunctionVariable,
						this.cCodeTranslator.translate(code2Translate)),
				dataOutReturn);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateSequentialOperation(Operation operation) {
		Variable userFunctionVariable = operation.getUserFunctionData().arguments[0];
		String code2Translate = operation.getUserFunctionData().Code.trim();
		code2Translate = this.removeCurlyBraces(code2Translate);
		ST st = new ST(templateOperationSequentialFunction);
		st.add("functionSignature",
				this.getOperationFunctionSignature(operation));
		String cCode = this.translateVariable(userFunctionVariable,
				this.cCodeTranslator.translate(code2Translate)).trim();
		ST stForY = new ST(templateForLoop);
		// BitmapImage and HDRImage types contains two for loops
		String dataInDeclaration;
		String dataOutReturn;
		if (operation.variable.typeName.equals(BitmapImage.getName())
				|| operation.variable.typeName.equals(HDRImage.getName())) {
			stForY.add("varName", "PM_y");
			stForY.add("varMaxVal", this.getHeightVariableName());
			dataInDeclaration = this.commonDefinitions
					.translateType(userFunctionVariable.typeName)
					+ " "
					+ userFunctionVariable.name
					+ " = "
					+ this.getDataVariableName() + "[PM_y*PM_width+PM_x];\n";
			dataOutReturn = this.getDataVariableName()
					+ "[PM_y*PM_width+PM_x] = " + userFunctionVariable.name
					+ ";\n";
			cCode = dataInDeclaration + cCode + "\n" + dataOutReturn;
			ST stForX = new ST(templateForLoop);
			stForX.add("varName", "PM_x");
			stForX.add("varMaxVal", this.getWidthVariableName());
			stForX.add("body", cCode);
			stForY.add("body", stForX.render());
		} else {
			// Array
			stForY.add("varName", "PM_x");
			stForY.add("varMaxVal", this.getLengthVariableName());
			dataInDeclaration = this.commonDefinitions
					.translateType(userFunctionVariable.typeName)
					+ " "
					+ userFunctionVariable.name
					+ " = "
					+ this.getDataVariableName() + "[PM_x];\n";
			dataOutReturn = this.getDataVariableName() + "[PM_x] = "
					+ userFunctionVariable.name + ";\n";
			cCode = dataInDeclaration + cCode + "\n" + dataOutReturn;
			// Array types
			stForY.add("body", cCode);
		}
		st.add("forLoop", stForY.render());
		// Each external variable value must be fill its equivalent output
		// variable so its value can be taken back to JVM.
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("externalVariables.{outVariableName, variableName}",
					this.commonDefinitions.getPrefix() + variable.name,
					variable.name);
		}
		return st.render();
	}

	private String removeCurlyBraces(String code) {
		// Remove the last curly brace
		code = code.substring(0, code.lastIndexOf("}"));
		// Remove the first curly brace
		code = code.substring(code.indexOf("{") + 1, code.length());
		return code;
	}

	/**
	 * Creates kernel declaration for a given operation.
	 */
	private String getOperationFunctionSignature(Operation operation) {
		ST st = new ST(templateKernelDecl);
		st.add("return", "void");
		st.add("functionName",
				this.commonDefinitions.getOperationName(operation));
		st.addAggr("params.{type, name}", String.format("__global %s*",
				this.commonDefinitions.translateType(operation
						.getUserFunctionData().arguments[0].typeName)), this
				.getDataVariableName());

		// External variables must be declared twice in sequential operations:
		// 1: A C typed variable with the same name which will be used in
		// the original user code.
		// 2: A global pointer which will take the processed value to the JVM
		// again;
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("params.{type, name}",
					PrimitiveTypes.getCType(variable.typeName), variable.name);
			if (operation.getExecutionType() == ExecutionType.Sequential) {
				st.addAggr(
						"params.{type, name}",
						"__global "
								+ PrimitiveTypes.getCType(variable.typeName)
								+ "*", this.commonDefinitions.getPrefix()
								+ variable);
			}
		}
		if (operation.getExecutionType() == ExecutionType.Sequential) {
			if (operation.variable.typeName.equals(BitmapImage.getName())
					|| operation.variable.typeName.equals(HDRImage.getName())) {
				st.addAggr("params.{type, name}", "int",
						this.getWidthVariableName());
				st.addAggr("params.{type, name}", "int",
						this.getHeightVariableName());
			} else if (operation.variable.typeName.equals(Array.getName())) {
				st.addAggr("params.{type, name}", "int",
						this.getLengthVariableName());
			}
		}

		return st.render();
	}

	/**
	 * Name for data variable that is used to write user array or image data in
	 * C kernel code.
	 */
	protected String getDataVariableName() {
		return this.commonDefinitions.getPrefix() + "data";
	}

	/**
	 * Name for height variable that is used in C kernel code.
	 */
	protected String getHeightVariableName() {
		return this.commonDefinitions.getPrefix() + "height";
	}

	/**
	 * Name for width variable that is used in C kernel code.
	 */
	protected String getWidthVariableName() {
		return this.commonDefinitions.getPrefix() + "width";
	}

	/**
	 * Name for length variable that is used in C kernel code.
	 */
	protected String getLengthVariableName() {
		return this.commonDefinitions.getPrefix() + "length";
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateOperationCall(String className, Operation operation) {
		ST st = new ST(templateOperationCall);
		st.add("operationName",
				this.commonDefinitions.getOperationName(operation));
		st.addAggr("params.{name}",
				"ParallelMERuntime.getInstance().runtimePointer");
		st.addAggr("params.{name}",
				this.commonDefinitions.getPointerName(operation.variable));
		if (operation.getExecutionType() == ExecutionType.Sequential) {
			for (Variable variable : operation.getExternalVariables()) {
				st.addAggr("params.{name}", variable.name + "[0]");
				st.addAggr("params.{name}", variable.name);
			}
		} else {
			for (Variable variable : operation.getExternalVariables()) {
				st.addAggr("params.{name}", variable.name);
			}
		}
		if (operation.destinationVariable != null) {
			st.addAggr("destinationVariable.{type, name}",
					operation.destinationVariable.typeName,
					operation.destinationVariable.name);
		} else {
			st.add("destinationVariable", null);
		}
		return st.render();
	}
}
