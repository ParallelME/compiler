/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
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
	private static final String templateFunctionDecl = "<isKernel:{var|__kernel}> <returnType> <functionName>(<params:{var|<var.type> <var.name>}; separator=\", \">)";
	protected static final String templateForLoop = "for (int <varName>=<initValue>; <varName> \\< <varMaxVal>; ++<varName>) {\n\t<body>}\n";
	private static final String templateSequentialForeach = "<functionSignature>\n {\n"
			+ "\t<forLoop>"
			+ "\t<externalVariables:{var|*<var.outVariableName> = <var.variableName>;\n}>"
			+ "}";
	private static final String templateParallelReduceTile = "\tint <gidVar> = get_global_id(0);\n"
			+ "\tint <baseVar> = <gidVar> * <sizeVar>;\n"
			+ "\t<varType> <inputVar1> = <dataVar>[<baseVar>];\n"
			+ "\t<varType> <inputVar2>;\n"
			+ "\tfor (int <xVar>=1; <xVar>\\<<sizeVar>; ++<xVar>) {\n"
			+ "\t\t<inputVar2> = <dataVar>[<baseVar> + <xVar>];\n"
			+ "\t\t<inputVar1> = <userFunctionName>(<inputVar1>, <inputVar2><params:{var|, <var.name>}>);\n"
			+ "\t}\n" + "\t<destinationVar>[<gidVar>] = <inputVar1>;\n";
	protected static final String templateReduce = "<decl:{var|\t\t<var.expression>;\n}>"
			+ "\t<forLoop:{var|<var.loop>}>"
			+ "\t*<destinationVar> = <inputVar1>;\n";
	protected static final String templateReduceForBody = "<inputVar2> = <dataVar>[<xVar>];\n"
			+ "<inputVar1> = <userFunctionName>(<inputVar1>, <inputVar2><params:{var|, <var.name>}>);\n";

	protected CTranslator cCodeTranslator;

	public PMTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelForeach(Operation operation) {
		List<String> variableDeclarations = new ArrayList<>();
		List<String> dataReturnStatements = new ArrayList<>();
		String gid = this.commonDefinitions.getPrefix() + "gid";
		for (Variable userFunctionVariable : operation.getUserFunctionData().arguments) {
			variableDeclarations
					.add(String.format(
							"%s %s = %s[%s];",
							this.commonDefinitions
									.translateType(userFunctionVariable.typeName),
							userFunctionVariable.name, this
									.getDataVariableName(), gid));
			dataReturnStatements
					.add(String.format("%s[%s] = %s;\n",
							this.getDataVariableName(), gid,
							userFunctionVariable.name));
		}
		StringBuilder ret = new StringBuilder();
		ret.append(this.getOperationFunctionSignature(operation,
				FunctionType.BaseOperation));
		ret.append(" {\n");
		ret.append(String.format("\tint %s = get_global_id(0);\n", gid));
		for (String variableDeclaration : variableDeclarations) {
			ret.append("\t" + variableDeclaration);
			ret.append("\n");
		}
		ret.append(this.translateUserCode(operation));
		ret.append("\n");
		for (String dataReturnStatement : dataReturnStatements)
			ret.append("\t" + dataReturnStatement);
		ret.append("}");
		return ret.toString();
	}

	protected String getExpression(String varType, String varName,
			String attributedVar) {
		ST st = new ST("<type> <name><attr:{var| = <var.expression>}>");
		st.add("type", varType);
		st.add("name", varName);
		st.add("attr", null);
		if (attributedVar != null && !attributedVar.isEmpty()) {
			st.addAggr("attr.{expression}", attributedVar);
		}
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelReduceTile(Operation operation) {
		ST st = new ST(templateParallelReduceTile);
		st.add("xVar", this.commonDefinitions.getPrefix() + "x");
		st.add("gidVar", this.commonDefinitions.getPrefix() + "gid");
		Variable inputVar1 = operation.getUserFunctionData().arguments.get(0);
		Variable inputVar2 = operation.getUserFunctionData().arguments.get(1);
		st.add("inputVar1", inputVar1.name);
		st.add("inputVar2", inputVar2.name);
		// Takes the first var, since they must be the same for reduce
		// operations
		st.add("varType",
				this.commonDefinitions.translateType(inputVar1.typeName));
		st.add("userFunctionName",
				this.commonDefinitions.getOperationUserFunctionName(operation));
		st.add("dataVar", this.getDataVariableName());
		st.add("baseVar", this.getBaseVariableName());
		if (operation.variable.typeName.equals(BitmapImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(HDRImage.getInstance()
						.getClassName())) {
			st.add("sizeVar", this.getWidthVariableName());
		} else {
			st.add("sizeVar", this.getTileSizeVariableName());
		}
		st.add("params", null);
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("params.{name}", variable.name);
		}
		st.add("destinationVar", this.getTileVariableName());
		return this.createKernelFunction(operation, st.render(),
				FunctionType.Tile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateReduceUserFunction(Operation operation) {
		return this.createKernelFunction(operation,
				this.translateUserCode(operation), FunctionType.UserCode);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateSequentialForeach(Operation operation) {
		Variable userFunctionVariable = operation.getUserFunctionData().arguments
				.get(0);
		String code2Translate = operation.getUserFunctionData().Code.trim();
		code2Translate = this.commonDefinitions
				.removeCurlyBraces(code2Translate);
		ST st = new ST(templateSequentialForeach);
		st.add("functionSignature", this.getOperationFunctionSignature(
				operation, FunctionType.BaseOperation));
		String cCode = this.translateVariable(userFunctionVariable,
				this.cCodeTranslator.translate(code2Translate)).trim();
		ST stForY = new ST(templateForLoop);
		stForY.add("initValue", "0");
		// BitmapImage and HDRImage types contains two for loops
		String dataInDeclaration;
		String dataOutReturn;
		String prefix = this.commonDefinitions.getPrefix();
		String x = prefix + "x";
		String y = prefix + "y";
		if (operation.variable.typeName.equals(BitmapImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(HDRImage.getInstance()
						.getClassName())) {
			stForY.add("varName", y);
			stForY.add("varMaxVal", this.getHeightVariableName());
			dataInDeclaration = String.format("%s %s = %s[%s*%s+%s];\n",
					this.commonDefinitions
							.translateType(userFunctionVariable.typeName),
					userFunctionVariable.name, this.getDataVariableName(), y,
					this.getWidthVariableName(), x);
			dataOutReturn = String.format("%s[%s*%s+%s] = %s;\n",
					this.getDataVariableName(), y, this.getWidthVariableName(),
					x, userFunctionVariable.name);
			cCode = dataInDeclaration + cCode + "\n" + dataOutReturn;
			ST stForX = new ST(templateForLoop);
			stForX.add("initValue", "0");
			stForX.add("varName", x);
			stForX.add("varMaxVal", this.getWidthVariableName());
			stForX.add("body", cCode);
			stForY.add("body", stForX.render());
		} else {
			// Array
			stForY.add("varName", x);
			stForY.add("varMaxVal", this.getLengthVariableName());
			dataInDeclaration = String.format("%s %s = %s[%s];\n",
					this.commonDefinitions
							.translateType(userFunctionVariable.typeName),
					userFunctionVariable.name, this.getDataVariableName(), x);
			dataOutReturn = String.format("%s[%s] = %s;\n",
					this.getDataVariableName(), x, userFunctionVariable.name);
			cCode = dataInDeclaration + cCode + "\n" + dataOutReturn;
			// Array types
			stForY.add("body", cCode);
		}
		st.add("forLoop", stForY.render());
		// Each external non-final variable value must fill its equivalent
		// output
		// variable so its value can be taken back to JVM.
		st.add("externalVariables", null);
		for (Variable variable : operation.getExternalVariables()) {
			if (!variable.isFinal())
				st.addAggr("externalVariables.{outVariableName, variableName}",
						this.commonDefinitions.getPrefix() + variable.name,
						variable.name);
		}
		return st.render();
	}

	/**
	 * Creates kernel declaration for a given operation.
	 */
	@Override
	protected String getOperationFunctionSignature(Operation operation,
			FunctionType functionType) {
		ST st;
		if (operation.operationType == OperationType.Foreach) {
			st = this.initializeForeachSignatureTemplate(operation,
					functionType);
		} else if (operation.operationType == OperationType.Reduce) {
			st = this
					.initializeReduceSignatureTemplate(operation, functionType);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		// External variables must be declared twice in sequential operations:
		// 1: A C typed variable with the same name which will be used in
		// the original user code.
		// 2: A global pointer which will take the processed value to the JVM
		// again;
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("params.{type, name}",
					PrimitiveTypes.getCType(variable.typeName), variable.name);
			if (isSequential && !variable.isFinal()) {
				st.addAggr(
						"params.{type, name}",
						"__global "
								+ PrimitiveTypes.getCType(variable.typeName)
								+ "*", this.commonDefinitions.getPrefix()
								+ variable);
			}
		}
		return st.render();
	}

	private ST initializeForeachSignatureTemplate(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		st.add("returnType", "void");
		st.add("isKernel", "");
		st.add("functionName",
				this.commonDefinitions.getOperationName(operation));
		st.addAggr("params.{type, name}", String.format("__global %s*",
				this.commonDefinitions.translateType(operation
						.getUserFunctionData().arguments.get(0).typeName)),
				this.getDataVariableName());
		if (operation.getExecutionType() == ExecutionType.Sequential)
			addSizeParams(operation, st);
		return st;
	}

	private ST initializeReduceSignatureTemplate(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		boolean isImage = operation.variable.typeName.equals(BitmapImage
				.getInstance().getClassName())
				|| operation.variable.typeName.equals(HDRImage.getInstance()
						.getClassName());
		String reduceType = this.commonDefinitions.translateType(operation
				.getUserFunctionData().arguments.get(0).typeName);
		if (functionType == FunctionType.BaseOperation) {
			st.add("returnType", "void");
			st.add("isKernel", "");
			st.add("functionName",
					this.commonDefinitions.getOperationName(operation));
			String destVar = commonDefinitions.getPrefix()
					+ operation.destinationVariable.name;
			st.addAggr("params.{type, name}",
					String.format("__global %s*", reduceType), destVar);
			String dataVar = operation.getExecutionType() != ExecutionType.Sequential
					&& isImage ? getTileVariableName() : getDataVariableName();
			st.addAggr("params.{type, name}",
					String.format("__global %s*", reduceType), dataVar);
			if (!isSequential) {
				if (isImage) {
					st.addAggr("params.{type, name}", "int",
							this.getHeightVariableName());
				} else {
					st.addAggr("params.{type, name}", String.format("__global %s*", reduceType),
							getTileVariableName());
					st.addAggr("params.{type, name}", "int",
							getLengthVariableName());
					st.addAggr("params.{type, name}", "int",
							getTileSizeVariableName());
				}
			}
		} else if (functionType == FunctionType.Tile) {
			st.add("returnType", "void");
			st.add("isKernel", "");
			st.add("functionName", this.commonDefinitions
					.getOperationTileFunctionName(operation));
			st.addAggr("params.{type, name}",
					String.format("__global %s*", reduceType),
					this.getDataVariableName());
			st.addAggr("params.{type, name}",
					String.format("__global %s*", reduceType),
					this.getTileVariableName());
			if (isImage) {
				st.addAggr("params.{type, name}", "int",
						this.getWidthVariableName());
			} else {
				st.addAggr("params.{type, name}", "int",
						this.getTileSizeVariableName());
			}
		} else {
			// Considering there are only 3 function types, the third type
			// is considered here
			st.add("returnType", this.commonDefinitions
					.translateType(operation.destinationVariable.typeName));
			st.add("isKernel", null);
			st.add("functionName", this.commonDefinitions
					.getOperationUserFunctionName(operation));
			Variable inputVar1 = operation.getUserFunctionData().arguments
					.get(0);
			Variable inputVar2 = operation.getUserFunctionData().arguments
					.get(1);
			st.addAggr("params.{type, name}",
					this.commonDefinitions.translateType(inputVar1.typeName),
					inputVar1.name);
			st.addAggr("params.{type, name}",
					this.commonDefinitions.translateType(inputVar2.typeName),
					inputVar2.name);
		}
		// Functions that encapsulate user code doesn't need
		if (isSequential && functionType != FunctionType.UserCode) {
			addSizeParams(operation, st);
		}
		return st;
	}

	/**
	 * Add size parameters to a given string template based on the operation
	 * variable type.
	 */
	private void addSizeParams(Operation operation, ST st) {
		if (operation.variable.typeName.equals(BitmapImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(HDRImage.getInstance()
						.getClassName())) {
			st.addAggr("params.{type, name}", "int",
					this.getWidthVariableName());
			st.addAggr("params.{type, name}", "int",
					this.getHeightVariableName());
		} else {
			st.addAggr("params.{type, name}", "int",
					this.getLengthVariableName());
		}
	}

	/**
	 * Translates a given operation's user code.
	 */
	private String translateUserCode(Operation operation) {
		String userCode = this.commonDefinitions.removeCurlyBraces(operation
				.getUserFunctionData().Code.trim());
		for (Variable userFunctionVariable : operation.getUserFunctionData().arguments) {
			userCode = this.translateVariable(userFunctionVariable,
					this.cCodeTranslator.translate(userCode));
		}
		return userCode;
	}

	/**
	 * Name for data variable that is used to write user array or image data in
	 * C kernel code.
	 */
	protected String getDataVariableName() {
		return this.commonDefinitions.getPrefix() + "data";
	}

	/**
	 * Name for tile variable that is used to write temporary tile data in C
	 * kernel code.
	 */
	protected String getTileVariableName() {
		return this.commonDefinitions.getPrefix() + "tile";
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
	 * Name for worksize variable that is used in C kernel code.
	 */
	protected String getWorkSizeVariableName() {
		return this.commonDefinitions.getPrefix() + "workSize";
	}

	/**
	 * Name for worksize variable that is used in C kernel code.
	 */
	protected String getTileSizeVariableName() {
		return this.commonDefinitions.getPrefix() + "tileSize";
	}

	/**
	 * Fill basic information about this operation call in the informed ST
	 * object. Method created to avoid code duplication in PMTranslator and
	 * PMImageTranslator.
	 */
	protected void fillOperationCallBaseInfo(ST st, Operation operation) {
		st.add("operationName",
				this.commonDefinitions.getOperationName(operation));
		st.addAggr("params.{name}",
				"ParallelMERuntime.getInstance().runtimePointer");
		st.addAggr("params.{name}",
				this.commonDefinitions.getPointerName(operation.variable));
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		for (Variable variable : operation.getExternalVariables()) {
			if (isSequential && !variable.isFinal())
				st.addAggr("params.{name}", variable.name + "[0]");
			st.addAggr("params.{name}", variable.name);
		}
	}
}
