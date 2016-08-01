/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.Pixel;
import org.stringtemplate.v4.ST;

/**
 * Base class for ParallelME runtime translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMTranslator extends BaseUserLibraryTranslator {
	protected static final String templateCallJNIFunction = "<jniJavaClassName>.getInstance().<functionName>(<params:{var|<var.name>}; separator=\", \">)";
	private static final String templateFunctionDecl = "<modifier:{var|<var.value> }><isKernel:{var|__kernel }><returnType> "
			+ "<functionName>(<params:{var|<var.type> <var.name>}; separator=\", \">)";
	protected static final String templateForLoop = "for (int <varName>=<initValue>; <varName> \\< <varMaxVal>; ++<varName>) {\n\t<body>}\n";
	private static String templateParallelForeachMapFunction = "<isImage:{var|\tint <xVar> = get_global_id(0);\n"
			+ "\tint <yVar> = get_global_id(1);\n"
			+ "\tint <varGID> = <yVar> * <xSizeVar> + <xVar>;\n}>"
			+ "<isArray:{var|\tint <varGID> = get_global_id(0);\n}>"
			+ "\t<destVarName>[<varGID>] = <userFunction>(<varName>[<varGID>]<isImage:{var|, <xVar>, <yVar>}><params:{var|, <var.name>}>);\n";
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
	private static String templateSequentialFunction = "\tfor (int <xVar>=0; <xVar>\\<<xSizeVar>; ++<xVar>) {\n"
			+ "<isImage:{var|\tfor (int <yVar>=0; <yVar>\\<<ySizeVar>; ++<yVar>) {\n}>"
			+ "\t\t\t<body>\n" + "<isImage:{var|\t\\}\n}>" + "\t}\n";
	private static String templateCallUserFunction = "<isImage:{var|int <varGID> = <xVar>+<yVar>*<xSizeVar>;\n}>"
			+ "<destVarName>[<isArray:{var|<xVar>}><isImage:{var|<varGID>}>] = "
			+ "<userFunction>(<varName>[<isArray:{var|<xVar>}><isImage:{var|<varGID>}>]<isImage:{var|, <xVar>, <yVar>}><params:{var|, <var.name>}>);";
	private static final String templateOperationCall = "<destinationVariable:{var|<var.nativeReturnType>[] <var.name> = new <var.nativeReturnType>[<var.size>];\n}>"
			+ "<initPointer:{var|<var.destName> = ParallelMERuntime.getInstance().createArray(<var.mapClassType>.class, \n\t<var.sizeExpression>);\n}>"
			+ "<operationName>(<params:{var|<var.name>}; separator=\", \">);"
			+ "<fromImage:{var|\n\n<var.name> = <var.value>;}>"
			+ "<destinationVariable:{var|\n\nreturn new <var.methodReturnType>(<var.expression>);}>";

	protected CTranslator cCodeTranslator;

	public PMTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateOperationCall(String className, Operation operation) {
		ST st = new ST(templateOperationCall);
		st.add("fromImage", null);
		st.add("initPointer", null);
		st.add("destinationVariable", null);
		st.add("operationName", commonDefinitions.getOperationName(operation));
		st.addAggr("params.{name}",
				"ParallelMERuntime.getInstance().runtimePointer");
		st.addAggr("params.{name}",
				commonDefinitions.getPointerName(operation.variable));
		if (operation.operationType == OperationType.Reduce) {
			fillReduceOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Foreach) {
			fillForeachOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Map) {
			fillMapOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Filter) {
			fillFilterOperationCall(st, operation);
		}
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("params.{name}", variable.name);
		}
		return st.render();
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid reduce operation call.
	 */
	protected void fillReduceOperationCall(ST st, Operation operation) {
		String variableName = commonDefinitions.getPrefix()
				+ operation.destinationVariable.name;
		String nativeReturnType = commonDefinitions
				.translateToJavaType(operation.destinationVariable.typeName);
		boolean isImage = commonDefinitions.isImage(operation.variable);
		String size = isImage ? "4" : "1";
		String methodReturnType = commonDefinitions
				.getUserLibraryReturnType(operation);
		ST stExpression = new ST("<params:{var|<var.value>}; separator=\", \">");
		stExpression.addAggr("params.{value}", variableName + "[0]");
		if (isImage) {
			stExpression.addAggr("params.{value}", variableName + "[1]");
			stExpression.addAggr("params.{value}", variableName + "[2]");
			stExpression.addAggr("params.{value}", variableName + "[3]");
			stExpression.addAggr("params.{value}", "-1");
			stExpression.addAggr("params.{value}", "-1");
		}
		st.addAggr(
				"destinationVariable.{name, nativeReturnType, size, methodReturnType, expression}",
				variableName, nativeReturnType, size, methodReturnType,
				stExpression.render());
		st.addAggr("params.{name}", variableName);
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid foreach operation call.
	 */
	protected void fillForeachOperationCall(ST st, Operation operation) {
		String fromImageVar = commonDefinitions
				.getFromImageBooleanName(operation.variable);
		st.addAggr("fromImage.{name, value}", fromImageVar, commonDefinitions
				.isImage(operation.variable) ? "true" : "false");
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid map operation call.
	 */
	protected void fillMapOperationCall(ST st, Operation operation) {
		st.addAggr("params.{name}",
				commonDefinitions.getPointerName(operation.destinationVariable));

		boolean isImage = commonDefinitions.isImage(operation.variable);
		String pointerName = commonDefinitions
				.getPointerName(operation.variable);
		String sizeExpression;
		if (isImage) {
			sizeExpression = String
					.format("ParallelMERuntime.getInstance().getWidth(%s) * ParallelMERuntime.getInstance().getHeight(%s)",
							pointerName, pointerName);
		} else {
			sizeExpression = String.format(
					"ParallelMERuntime.getInstance().getLength(%s)",
					pointerName);
		}
		st.addAggr(
				"initPointer.{destName, mapClassType, sizeExpression}",
				commonDefinitions.getPointerName(operation.destinationVariable),
				commonDefinitions.getJavaReturnType(operation), sizeExpression);
		String fromImageVar = commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable);
		st.addAggr("fromImage.{name, value}", fromImageVar, isImage ? "true"
				: "false");
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid filter operation call.
	 */
	protected void fillFilterOperationCall(ST st, Operation operation) {
		String fromImageVar = commonDefinitions
				.getFromImageBooleanName(operation.destinationVariable);
		st.addAggr("fromImage.{name, value}", fromImageVar, commonDefinitions
				.isImage(operation.variable) ? "true" : "false");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateObjDeclaration(InputBind inputBind) {
		return translateObjDeclaration(inputBind.variable);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateObjDeclaration(Operation operation) {
		if (operation.operationType == OperationType.Map
				|| operation.operationType == OperationType.Filter
				|| operation.operationType == OperationType.Foreach) {
			return translateObjDeclaration(operation.destinationVariable);
		} else {
			return "";
		}
	}

	/**
	 * Returns the creation statement for a given variable's allocation.
	 */
	private String translateObjDeclaration(Variable variable) {
		// The "FromImage" boolean is used to control if the last call for a
		// given variable is originated from an image element. In case
		// positive, the outputbind must multiply by 4 the lenght of the
		// returning array.
		return String.format("private long %s;\nprivate boolean %s = false;",
				commonDefinitions.getPointerName(variable),
				commonDefinitions.getFromImageBooleanName(variable));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateForeach(Operation operation) {
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			return translateParallelForeachMap(operation);
		} else {
			return translateSequentialForeachMap(operation);
		}
	}

	/**
	 * Translates a parallel foreach or map operation returning a C code
	 * compatible with this runtime.
	 */
	private String translateParallelForeachMap(Operation operation) {
		ST st = new ST(templateParallelForeachMapFunction);
		st.add("userFunction",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("varGID", getGIDVariableName());
		st.add("varName", commonDefinitions.getDataVarName());
		if (operation.operationType == OperationType.Map) {
			st.add("destVarName", commonDefinitions.getDataReturnVarName());
		} else {
			st.add("destVarName", commonDefinitions.getDataVarName());
		}
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("isImage", "");
			st.add("isArray", null);
			String prefix = commonDefinitions.getPrefix();
			st.add("xVar", prefix + "x");
			st.add("yVar", prefix + "y");
			st.add("xSizeVar", getWidthVariableName());
		} else {
			st.add("isImage", null);
			st.add("isArray", "");
		}
		setExternalVariables(st, operation, false);
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	/**
	 * Translates a sequential foreach or map operation returning a C code
	 * compatible with this runtime.
	 */
	private String translateSequentialForeachMap(Operation operation) {
		ST st = new ST(templateSequentialFunction);
		setCommonParameters(operation, st);
		ST stBody = new ST(templateCallUserFunction);
		setCommonParameters(operation, stBody);
		stBody.add("userFunction",
				commonDefinitions.getOperationUserFunctionName(operation));
		stBody.add("varGID", getGIDVariableName());
		stBody.add("varName", commonDefinitions.getDataVarName());
		if (operation.operationType == OperationType.Map) {
			stBody.add("destVarName", commonDefinitions.getDataReturnVarName());
		} else {
			stBody.add("destVarName", commonDefinitions.getDataVarName());
		}
		if (commonDefinitions.isImage(operation.variable)) {
			stBody.add("idxVar", commonDefinitions.getPrefix() + "idx");
			stBody.add("isArray", null);
		} else {
			stBody.add("isArray", "");
		}
		setExternalVariables(stBody, operation, false);
		st.add("body", stBody.render());
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	private void setCommonParameters(Operation operation, ST st) {
		String prefix = commonDefinitions.getPrefix();
		st.add("xVar", prefix + "x");
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("isImage", "");
			st.add("yVar", prefix + "y");
			st.add("xSizeVar", getWidthVariableName());
			st.add("ySizeVar", getHeightVariableName());
		} else {
			st.add("isImage", null);
			st.add("xSizeVar", getLengthVariableName());
		}
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
		String prefix = commonDefinitions.getPrefix();
		st.add("xVar", prefix + "x");
		st.add("gidVar", prefix + "gid");
		Variable inputVar1 = operation.getUserFunctionData().arguments.get(0);
		Variable inputVar2 = operation.getUserFunctionData().arguments.get(1);
		st.add("inputVar1", inputVar1.name);
		st.add("inputVar2", inputVar2.name);
		// Takes the first var, since they must be the same for reduce
		// operations
		st.add("varType", commonDefinitions.getCReturnType(operation));
		st.add("userFunctionName",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("dataVar", commonDefinitions.getDataVarName());
		st.add("baseVar", getBaseVariableName());
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("sizeVar", getWidthVariableName());
		} else {
			st.add("sizeVar", getTileSizeVariableName());
		}
		st.add("params", null);
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("params.{name}", variable.name);
		}
		st.add("destinationVar", getTileVariableName());
		return createKernelFunction(operation, st.render(), FunctionType.Tile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateMap(Operation operation) {
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			return translateParallelForeachMap(operation);
		} else {
			return translateSequentialForeachMap(operation);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateFilter(Operation operation) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateFilterTile(Operation operation) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateUserFunction(Operation operation) {
		String userCode = commonDefinitions.removeCurlyBraces(operation
				.getUserFunctionData().Code.trim());
		for (Variable userFunctionVariable : operation.getUserFunctionData().arguments) {
			userCode = this.translateVariable(userFunctionVariable,
					this.cCodeTranslator.translate(userCode));
		}
		// Replace non-final variables by its equivalent pointer
		for (Variable variable : operation.getExternalVariables()) {
			if (!variable.isFinal()) {
				userCode = userCode.replaceAll(variable.name, "*"
						+ commonDefinitions.getPrefix() + variable.name);
			}
		}
		// Foreach operations must add a return statements.
		if (operation.operationType == OperationType.Foreach) {
			userCode += String.format("\treturn %s;\n",
					operation.getUserFunctionData().arguments.get(0));
		}
		return this.createKernelFunction(operation, userCode,
				FunctionType.UserCode);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String initializeUserFunctionSignature(Operation operation) {
		ST st = new ST(templateFunctionDecl);
		st.add("functionName",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("returnType", commonDefinitions.getCReturnType(operation));
		st.addAggr("modifier.{value}", "static");
		st.add("isKernel", null);
		st.add("params", null);
		for (Variable param : operation.getUserFunctionData().arguments) {
			String paramType = commonDefinitions
					.translateToCType(param.typeName);
			st.addAggr("params.{type, name}", paramType, param.name);
		}
		if (commonDefinitions.isImage(operation.variable)
				&& operation.operationType != OperationType.Reduce) {
			st.addAggr("params.{type, name}", "int", "x");
			st.addAggr("params.{type, name}", "int", "y");
		}
		setExternalVariables(st, operation, true);
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String initializeForeachSignature(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		st.add("modifier", null);
		st.add("returnType", "void");
		st.add("isKernel", "");
		st.add("functionName", commonDefinitions.getOperationName(operation));
		st.addAggr(
				"params.{type, name}",
				String.format("__global %s*",
						commonDefinitions.getCReturnType(operation)),
				commonDefinitions.getDataVarName());
		addSizeParams(operation, st);
		setExternalVariables(st, operation, true);
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String initializeReduceSignature(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		st.add("modifier", null);
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		boolean isImage = commonDefinitions.isImage(operation.variable);
		String reduceType = commonDefinitions.translateToCType(operation
				.getUserFunctionData().arguments.get(0).typeName);
		if (functionType == FunctionType.BaseOperation) {
			st.add("returnType", "void");
			st.add("isKernel", "");
			st.add("functionName",
					commonDefinitions.getOperationName(operation));
			String destVar = commonDefinitions.getDataReturnVarName();
			st.addAggr("params.{type, name}",
					String.format("__global %s*", reduceType), destVar);
			String dataVar = operation.getExecutionType() != ExecutionType.Sequential
					&& isImage ? getTileVariableName() : commonDefinitions
					.getDataVarName();
			st.addAggr("params.{type, name}",
					String.format("__global %s*", reduceType), dataVar);
			if (!isSequential) {
				if (isImage) {
					st.addAggr("params.{type, name}", "int",
							getHeightVariableName());
				} else {
					st.addAggr("params.{type, name}",
							String.format("__global %s*", reduceType),
							getTileVariableName());
					st.addAggr("params.{type, name}", "int",
							getLengthVariableName());
					st.addAggr("params.{type, name}", "int",
							getTileSizeVariableName());
				}
			} else {
				addSizeParams(operation, st);
			}
		} else if (functionType == FunctionType.Tile) {
			st.add("returnType", "void");
			st.add("isKernel", "");
			st.add("functionName",
					commonDefinitions.getOperationTileFunctionName(operation));
			st.addAggr("params.{type, name}",
					String.format("__global %s*", reduceType),
					commonDefinitions.getDataVarName());
			st.addAggr("params.{type, name}",
					String.format("__global %s*", reduceType),
					this.getTileVariableName());
			if (isImage) {
				st.addAggr("params.{type, name}", "int", getWidthVariableName());
			} else {
				st.addAggr("params.{type, name}", "int",
						this.getTileSizeVariableName());
			}
		} else {
			// Considering there are only 3 function types, the third type
			// is considered here
			st.add("returnType", commonDefinitions
					.translateToCType(operation.destinationVariable.typeName));
			st.add("isKernel", null);
			st.add("functionName",
					commonDefinitions.getOperationUserFunctionName(operation));
			Variable inputVar1 = operation.getUserFunctionData().arguments
					.get(0);
			Variable inputVar2 = operation.getUserFunctionData().arguments
					.get(1);
			st.addAggr("params.{type, name}",
					commonDefinitions.translateToCType(inputVar1.typeName),
					inputVar1.name);
			st.addAggr("params.{type, name}",
					commonDefinitions.translateToCType(inputVar2.typeName),
					inputVar2.name);
		}
		setExternalVariables(st, operation, true);
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String initializeMapSignature(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		st.add("modifier", null);
		st.add("returnType", "void");
		st.add("isKernel", "");
		st.add("functionName", commonDefinitions.getOperationName(operation));
		st.addAggr(
				"params.{type, name}",
				String.format("__global %s*",
						commonDefinitions.getCReturnType(operation)),
				commonDefinitions.getDataReturnVarName());
		if (commonDefinitions.isImage(operation.variable)) {
			st.addAggr("params.{type, name}", String.format("__global %s*",
					commonDefinitions.translateToCType(Pixel.getInstance()
							.getClassName())), commonDefinitions
					.getDataVarName());
		} else {
			st.addAggr("params.{type, name}", String.format("__global %s*",
					commonDefinitions
							.translateToCType(operation.variable.typeParameters
									.get(0))), commonDefinitions
					.getDataVarName());
		}
		addSizeParams(operation, st);
		setExternalVariables(st, operation, true);
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String initializeFilterSignature(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		st.add("modifier", null);
		st.add("returnType", null);
		st.add("isKernel", null);
		st.add("functionName", null);
		st.add("params", null);
		setExternalVariables(st, operation, true);
		return st.render();
	}

	/**
	 * Set external variables to a given method call or signature. In case
	 * 'includeVariableType' is true, it will include external variables' types,
	 * as it is expected to build a method signature. Final variables will be
	 * declared normally, while non-final variables will be declared
	 */
	protected void setExternalVariables(ST st, Operation operation,
			boolean includeVariableType) {
		st.add("params", null);
		String params = includeVariableType ? "params.{type, name}"
				: "params.{name}";
		for (Variable variable : operation.getExternalVariables()) {
			if (variable.isFinal()) {
				if (includeVariableType) {
					st.addAggr(params,
							PrimitiveTypes.getCType(variable.typeName),
							variable.name);
				} else {
					st.addAggr(params, variable.name);
				}
			} else {
				String variableName = commonDefinitions.getPrefix() + variable;
				if (includeVariableType) {
					String type = "__global "
							+ PrimitiveTypes.getCType(variable.typeName) + "*";
					st.addAggr(params, type, variableName);
				} else {
					st.addAggr(params, variableName);
				}
			}
		}
	}

	/**
	 * Add size parameters to a given string template based on the operation
	 * variable type.
	 */
	private void addSizeParams(Operation operation, ST st) {
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			if (commonDefinitions.isImage(operation.variable)) {
				st.addAggr("params.{type, name}", "int", getWidthVariableName());
			}
		} else {
			if (commonDefinitions.isImage(operation.variable)) {
				st.addAggr("params.{type, name}", "int", getWidthVariableName());
				st.addAggr("params.{type, name}", "int",
						getHeightVariableName());
			} else {
				st.addAggr("params.{type, name}", "int",
						getLengthVariableName());
			}
		}
	}

	/**
	 * Name for GID variable that is used to store the position in the actual
	 * image or array pointer in C kernel code.
	 */
	protected String getGIDVariableName() {
		return commonDefinitions.getPrefix() + "gid";
	}

	/**
	 * Name for tile variable that is used to write temporary tile data in C
	 * kernel code.
	 */
	protected String getTileVariableName() {
		return commonDefinitions.getPrefix() + "tile";
	}

	/**
	 * Name for height variable that is used in C kernel code.
	 */
	protected String getHeightVariableName() {
		return commonDefinitions.getPrefix() + "height";
	}

	/**
	 * Name for width variable that is used in C kernel code.
	 */
	protected String getWidthVariableName() {
		return commonDefinitions.getPrefix() + "width";
	}

	/**
	 * Name for length variable that is used in C kernel code.
	 */
	protected String getLengthVariableName() {
		return commonDefinitions.getPrefix() + "length";
	}

	/**
	 * Name for worksize variable that is used in C kernel code.
	 */
	protected String getWorkSizeVariableName() {
		return commonDefinitions.getPrefix() + "workSize";
	}

	/**
	 * Name for worksize variable that is used in C kernel code.
	 */
	protected String getTileSizeVariableName() {
		return commonDefinitions.getPrefix() + "tileSize";
	}
}
