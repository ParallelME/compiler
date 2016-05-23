/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.translation.userlibrary.BaseTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Base class for ParallelME runtime translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMTranslator extends BaseTranslator {
	protected static final String templateCallJNIFunction = "<jniJavaClassName>.getInstance().<functionName>(<params:{var|<var.name>}; separator=\", \">)";
	private static final String templateKernelDecl = "__kernel void <functionName>(<params:{var|<var.type> <var.name>}; separator=\", \">)";
	private static final String templateForLoop = "for (int <varName> = 0; <varName> \\< <varMaxVal>; ++<varName>) {\n\t<body>}\n";
	private static final String templateIteratorCall = "<iteratorName>(<params:{var|<var.name>}; separator=\", \">);";

	protected CTranslator cCodeTranslator;

	public PMTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelIterator(Iterator iterator) {
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		String code2Translate = iterator.getUserFunctionData().Code.trim();
		code2Translate = this.removeCurlyBraces(code2Translate);
		String ret;
		String gidDeclaration = String.format("int $gid = get_global_id(0);\n"
				+ "\t%s %s = %s[$gid];",
				this.translateType(userFunctionVariable.typeName),
				userFunctionVariable.name, this.getDataVariableName());
		String dataOutReturn = String.format("%s[$gid] = %s;\n",
				this.getDataVariableName(), userFunctionVariable.name);
		ret = String.format("%s {\n\t%s %s %s\n}", this
				.getIteratorFunctionSignature(iterator), gidDeclaration, this
				.translateVariable(userFunctionVariable,
						this.cCodeTranslator.translate(code2Translate)),
				dataOutReturn);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateSequentialIterator(Iterator iterator) {
		Variable userFunctionVariable = iterator.getUserFunctionData().variableArgument;
		String code2Translate = iterator.getUserFunctionData().Code.trim();
		code2Translate = this.removeCurlyBraces(code2Translate);
		String templateIteratorSequentialFunction = "<functionSignature>\n {\n"
				+ "\t<forLoop>"
				+ "\t<externalVariables:{var|*<var.outVariableName> = <var.variableName>;\n}>"
				+ "}";
		ST st = new ST(templateIteratorSequentialFunction);
		st.add("functionSignature", this.getIteratorFunctionSignature(iterator));
		String cCode = this.translateVariable(userFunctionVariable,
				this.cCodeTranslator.translate(code2Translate)).trim();
		ST stForY = new ST(templateForLoop);
		stForY.add("varName", "$y");
		stForY.add("varMaxVal", this.getHeightVariableName());
		// BitmapImage and HDRImage types contains two for loops
		String dataInDeclaration;
		String dataOutReturn;
		if (iterator.variable.typeName.equals(BitmapImage.getName())
				|| iterator.variable.typeName.equals(HDRImage.getName())) {
			dataInDeclaration = this
					.translateType(userFunctionVariable.typeName)
					+ " "
					+ userFunctionVariable.name
					+ " = "
					+ this.getDataVariableName() + "[$y*$width+$x];\n";
			dataOutReturn = this.getDataVariableName() + "[$y*$width+$x] = "
					+ userFunctionVariable.name + ";\n";
			cCode = dataInDeclaration + cCode + "\n" + dataOutReturn;
			ST stForX = new ST(templateForLoop);
			stForX.add("varName", "$x");
			stForX.add("varMaxVal", this.getWidthVariableName());
			stForX.add("body", cCode);
			stForY.add("body", stForX.render());
		} else {
			dataInDeclaration = this
					.translateType(userFunctionVariable.typeName)
					+ " "
					+ userFunctionVariable.name
					+ " = "
					+ this.getDataVariableName() + "[$y];\n";
			dataOutReturn = this.getDataVariableName() + "[$y] = "
					+ userFunctionVariable.name + ";\n";
			cCode = dataInDeclaration + cCode + "\n" + dataOutReturn;
			// Array types
			stForY.add("body", cCode);
		}
		st.add("forLoop", stForY.render());
		// Each external variable value must be fill its equivalent output
		// variable so its value can be taken back to JVM.
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("externalVariables.{outVariableName, variableName}",
					this.commonDefinitions.getVariableOutName(variable),
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
	 * Creates kernel declaration for a given iterator.
	 */
	private String getIteratorFunctionSignature(Iterator iterator) {
		ST st = new ST(templateKernelDecl);
		st.add("return", "void");
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.addAggr("params.{type, name}", "__global float4",
				"*" + this.getDataVariableName());
		// External variables must be declared twice in sequential iterators:
		// 1: A C typed variable with the same name which will be used in
		// the original user code.
		// 2: A global pointer which will take the processed value to the JVM
		// again;
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{type, name}",
					PrimitiveTypes.getCType(variable.typeName), variable.name);
			if (iterator.getType() == IteratorType.Sequential) {
				st.addAggr(
						"params.{type, name}",
						"__global *"
								+ PrimitiveTypes.getCType(variable.typeName),
						this.commonDefinitions.getPrefix() + variable);
			}
		}
		if (iterator.getType() == IteratorType.Sequential) {
			if (iterator.variable.typeName.equals(BitmapImage.getName())
					|| iterator.variable.typeName.equals(HDRImage.getName())) {
				st.addAggr("params.{type, name}", "int",
						this.getWidthVariableName());
				st.addAggr("params.{type, name}", "int",
						this.getHeightVariableName());
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
	 * {@inheritDoc}
	 */
	public String translateIteratorCall(String className, Iterator iterator) {
		ST st = new ST(templateIteratorCall);
		st.add("iteratorName", this.commonDefinitions.getIteratorName(iterator));
		st.addAggr("params.{name}",
				this.commonDefinitions.getRuntimePointerName());
		st.addAggr("params.{name}",
				this.commonDefinitions.getPointerName(iterator.variable));
		if (iterator.getType() == IteratorType.Sequential) {
			for (Variable variable : iterator.getExternalVariables()) {
				st.addAggr("params.{name}", variable.name + "[0]");
				st.addAggr("params.{name}", variable.name);
			}
		} else {
			for (Variable variable : iterator.getExternalVariables()) {
				st.addAggr("params.{name}", variable.name);
			}
		}
		return st.render();
	}
}
