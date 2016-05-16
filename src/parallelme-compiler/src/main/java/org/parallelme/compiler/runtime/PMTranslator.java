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
	private static final String templateForLoop = "for (int <varName> = 0; <varName> \\< <varMaxVal>; <varName>++) {\n\t<body>}\n";
	protected static final String templateRunWait = "\n\nParallelMERuntimeJNIWrapper().getInstance().run(<bufferId>);\n"
			+ "ParallelMERuntimeJNIWrapper().getInstance().waitFinish();\n";
	// private static final

	protected CTranslator cCodeTranslator;

	public PMTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
	}

	protected String getJNIWrapperClassName(String className) {
		return className + "JNIWrapper";
	}

	protected String getInputBufferIdName(Variable variable) {
		return this.commonDefinitions.getVariableInName(variable) + "BufferId";
	}

	protected String getOutputBufferIdName(Variable variable) {
		return this.commonDefinitions.getVariableOutName(variable) + "BufferId";
	}

	protected String getOutputBufferDataName(Variable variable) {
		return this.commonDefinitions.getVariableOutName(variable)
				+ "DataBuffer";
	}

	protected String getWorksizeName(Variable variable) {
		return this.commonDefinitions.getPrefix() + variable.name + "Worksize";
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
		String dataInDeclaration = "\n\tint $gid = get_global_id(0);\n\t"
				+ this.translateType(userFunctionVariable.typeName) + " "
				+ userFunctionVariable.name + " = "
				+ this.getDataInVariableName() + "[$gid];";
		String dataOutReturn = this.getDataOutVariableName() + "[$gid] = "
				+ userFunctionVariable.name + ";\n";
		ret = this.getIteratorFunctionSignature(iterator)
				+ " {"
				+ dataInDeclaration
				+ this.translateVariable(userFunctionVariable,
						this.cCodeTranslator.translate(code2Translate))
				+ dataOutReturn + "\n}";
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
					+ this.getDataInVariableName() + "[$y*$width+$x];\n";
			dataOutReturn = this.getDataOutVariableName() + "[$y*$width+$x] = "
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
					+ this.getDataInVariableName() + "[$y];\n";
			dataOutReturn = this.getDataOutVariableName() + "[$y] = "
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
				"*" + this.getDataInVariableName());
		st.addAggr("params.{type, name}", "__global float4",
				"*" + this.getDataOutVariableName());
		// External variables must be declared twice:
		// 1: A global pointer which will take the processed value to the JVM
		// again;
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{type, name}",
					"__global *" + PrimitiveTypes.getCType(variable.typeName),
					this.commonDefinitions.getVariableOutName(variable));
		}
		if (iterator.getType() == IteratorType.Sequential) {
			if (iterator.variable.typeName.equals(BitmapImage.getName())
					|| iterator.variable.typeName.equals(HDRImage.getName())) {
				st.addAggr("params.{type, name}", "int",
						this.getHeightVariableName());
				st.addAggr("params.{type, name}", "int",
						this.getWidthVariableName());
			}
		}
		// ...and 2: A C typed variable with the same name which will be used in
		// the original user code.
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{type, name}",
					PrimitiveTypes.getCType(variable.typeName), variable.name);
		}

		return st.render();
	}

	/**
	 * Name for data input variable that is used to store user array or image
	 * data in C kernel code.
	 */
	protected String getDataInVariableName() {
		return this.commonDefinitions.getPrefix() + "dataIn";
	}

	/**
	 * Name for data output variable that is used to write user array or image
	 * data in C kernel code.
	 */
	protected String getDataOutVariableName() {
		return this.commonDefinitions.getPrefix() + "dataOut";
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
		StringBuilder ret = new StringBuilder();
		String templateIteratorCallParams = "<externalVariables:{var|<var.type>[] <var.name> = new <var.type>[1];\n"
				+ "int <var.bufferId> = ParallelMERuntimeJNIWrapper().getInstance().createAllocation(<var.name>, 1);\n}>";
		String templateIteratorCallParamsReturn = "<externalVariables:{\n\tvar|<var.name> = <var.bufferData>[0];\n}>";
		ST stCall = new ST(templateCallJNIFunction);
		stCall.add("jniJavaClassName", this.getJNIWrapperClassName(className));
		stCall.add("functionName",
				this.commonDefinitions.getIteratorName(iterator));
		stCall.addAggr("params.{name}",
				this.getOutputBufferIdName(iterator.variable));
		stCall.addAggr("params.{name}",
				this.getOutputBufferIdName(iterator.variable));
		stCall.addAggr("params.{name}", this.getWorksizeName(iterator.variable));
		for (Variable variable : iterator.getExternalVariables()) {
			stCall.addAggr("params.{name}", variable.name);
		}
		if (iterator.getExternalVariables().length > 0) {
			ST stParams = new ST(templateIteratorCallParams);
			ST stParamsReturn = new ST(templateIteratorCallParamsReturn);
			stParams.add("externalVariables", null);
			stParamsReturn.add("externalVariables", null);
			for (Variable variable : iterator.getExternalVariables()) {
				// Only non-final variables must have an allocation
				if (!variable.isFinal()) {
					stParams.addAggr(
							"externalVariables.{type, name, bufferId}",
							variable.typeName,
							this.getOutputBufferDataName(variable),
							this.getOutputBufferIdName(variable));
					stParamsReturn.addAggr(
							"externalVariables.{name, bufferData}",
							variable.name,
							this.getOutputBufferDataName(variable));
				}
			}
			ret.append(stParams.render());
			ret.append(stCall.render() + ";");
			if (iterator.getType() == IteratorType.Sequential) {
				stParams = new ST(templateRunWait);
				stParams.add("bufferId", this.getOutputBufferIdName(iterator
						.getExternalVariables()[0]));
				ret.append(stParams.render());
				ret.append(stParamsReturn.render());
			}
		} else {
			ret.append(stCall.render() + ";");
		}
		return ret.toString();
	}
}
