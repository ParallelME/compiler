/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.UserLibraryClass;
import org.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import org.parallelme.compiler.userlibrary.UserLibraryDataType;
import org.parallelme.compiler.userlibrary.classes.Pixel;
import org.stringtemplate.v4.ST;

/**
 * Base class for RenderScript translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSTranslator extends BaseUserLibraryTranslator {
	private static final String templateOperationCall = "<tileSize:{var|int <var.name> = <var.expression>;\n}>"
			+ "<allocation:{var|<var.body>}; separator=\"\\n\">"
			+ "<variables:{var|\n\n<kernelName>.set_<var.gVariableName>(<var.variableName>);}>"
			+ "<inputSize:{var|\n\n<kernelName>.set_<var.name>(<var.allocationName>.getType().get<var.XYZ>());}>"
			+ "<kernels:{var|\n\n<kernelName>.<var.rsOperationName>_<var.functionName>(<var.allocations>);}>"
			+ "<sequentialNonFinalVariables:{var|\n\n<var.allName>.copyTo(<var.arrName>);}>"
			+ "<additionalStatements:{var|\n\n<var.value>}>"
			+ "<destinationVariable:{var|\n\n<var.nativeReturnType>[] <var.tmpName> = new <var.nativeReturnType>[<var.size>];\n"
			+ "<var.name>.copyTo(<var.tmpName>);\n"
			+ "return new <var.returnObjectCreation>;}>";
	private static final String templateAllocationRSFile = "<allocation:{var|rs_allocation <var.name>;\n}>"
			+ "<sizeVar:{var|int <var.name>;\n}>"
			+ "<externalVariables:{var|<var.type> <var.name>;\n}>\n";
	private static final String templateSequentialAllocationJavaFile = "<externalVariables:{var|"
			+ "Allocation <var.allName> = Allocation.createSized(<rsVarName>, Element.<var.elementType>(<rsVarName>), 1);\n"
			+ "<kernelName>.set_<var.gName>(<var.name>[0]);\n"
			+ "<kernelName>.set_<var.gNameOut>(<var.allName>);}; separator=\"\\n\">";
	protected static final String templateReduceForBody = "<inputVar2> = rsGetElementAt_<varType>(<dataVar>, <xVar><yVar:{var|, <var.name>}>);\n"
			+ "<inputVar1> = <userFunctionName>(<inputVar1>, <inputVar2>);\n";
	protected static final String templateForLoop = "for (int <varName>=<initValue>; <varName> \\< <varMaxVal>; ++<varName>) {\n\t<body>}\n";
	protected static final String templateReduce = "\t<varType> <inputVar1> = rsGetElementAt_<varType>(<dataVar>, 0);\n"
			+ "\t<varType> <inputVar2>;\n"
			+ "\t<forLoop:{var|<var.loop>}>"
			+ "\trsSetElementAt_<varType>(<destVar>, <inputVar1>, 0);\n"
			+ "<setExternalVariables:{var|\t\trsSetElementAt_<var.type>(<var.allocationName>, <var.varName>, 0);\n}>";
	private static final String templateParallelReduceTile = "<declBaseVar:{var|\t\tint <baseVar> = x * <sizeVar>;\n}>"
			+ "\t<varType> <inputVar1> = rsGetElementAt_<varType>(<dataVar>, <baseVar>);\n"
			+ "\t<varType> <inputVar2>;\n"
			+ "\tfor (int <xVar>=1; <xVar>\\<<sizeVar>; ++<xVar>) {\n"
			+ "\t\t<inputVar2> = rsGetElementAt_<varType>(<dataVar>, <x:{var|x, }><declBaseVar:{var|<baseVar> + }><xVar>);\n"
			+ "\t\t<inputVar1> = <userFunctionName>(<inputVar1>, <inputVar2>);\n"
			+ "\t}\n" + "\treturn <inputVar1>;\n";
	private static final String templateFilter = "\tint <varCount> = 0;\n"
			+ "\tfor (int <xVar>=0; <xVar>\\<rsAllocationGetDimX(<tileAllocation>); ++<xVar>) {\n"
			+ "<isImage:{var|\tfor (int <yVar>=0; <yVar>\\<rsAllocationGetDimY(<tileAllocation>); ++<yVar>) {\n}>"
			+ "\t\tint PM_value = rsGetElementAt_int(<tileAllocation>, <xVar><isImage:{var|, <yVar>}>);\n"
			+ "\t\tif (PM_value > 0) {\n"
			+ "\t\t\trsSetElementAt_<type>(<outputAllocation>, rsGetElementAt_<type>(<inputAllocation>, PM_value), <varCount>++);\n"
			+ "\t\t}\n" + "<isImage:{var|\t\\}\n}>" + "\t}\n";
	private static final String templateParallelFilterTile = "\tif (<userFunctionName>(<varName>)) {\n"
			+ "\t\trsAtomicInc(&<varCounterName>);\n"
			+ "\t\treturn x;\n"
			+ "\t} else {\n" + "\t\treturn -1;\n" + "\t}\n";
	private static final String templateFunctionDecl = "<modifier:{var|<var.value> }><returnType><isKernel:{var|  __attribute__((kernel))}> <functionName>("
			+ "<params:{var|<var.type> <var.name>}; separator=\", \">)";
	private static final String templateAllocation = "Type <allocationName>Type = new Type.Builder(<rsVarName>, Element.<rsType>(<rsVarName>))\n"
			+ "\t.set<XYZ>(<expression>)\n"
			+ "\t.create();\n"
			+ "<declareAllocation:{var|Allocation }><allocationName> = Allocation.createTyped(<rsVarName>, <allocationName>Type);";
	private static String templateParallelForeachMapFunction = "\treturn <userFunction>("
			+ "<varName><isImage:{var|, x, y}>);\n";
	private static String templateSequentialFunction = "\tfor (int <xVar>=0; <xVar>\\<rsAllocationGetDimX(<readAllocation>); ++<xVar>) {\n"
			+ "<isImage:{var|\tfor (int <yVar>=0; <yVar>\\<rsAllocationGetDimY(<readAllocation>); ++<yVar>) {\n}>"
			+ "\t\t\t<body>\n"
			+ "<isImage:{var|\t\\}\n}>"
			+ "\t}\n"
			+ "<setExternalVariables:{var|\t\trsSetElementAt_<var.type>(<var.allocationName>, <var.varName>, 0);\n}>";
	private static String templateSetValues = "rsSetElementAt_<typeSet>(<writeAllocation>, <userFunction>(rsGetElementAt_<typeGet>("
			+ "<readAllocation>, <xVar><isImage:{var|, <yVar>}>)), <xVar><isImage:{var|, <yVar>}>);";
	private static String templateFilterOperationCall = "int <sizeVarName>[] = new int[1];\n"
			+ "<kernelName>.get_<gSizeVariableName>().copyTo(<sizeVarName>);\n"
			+ "if (<sizeVarName>[0] > 0) {\n"
			+ "\t<allocation:{var|<var.body>}; separator=\"\\n\">"
			+ "\t<variables:{var|\n\n\t<kernelName>.set_<var.gVariableName>(<var.variableName>);}>"
			+ "<kernels:{var|\n\n\t<kernelName>.<var.rsOperationName>_<var.functionName>(<var.allocations>);}>"
			+ "\n}";

	protected CTranslator cCodeTranslator;

	public RSTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateOperationCall(String className, Operation operation) {
		ST st = new ST(templateOperationCall);
		st.add("rsVarName", getRSVariableName());
		st.add("kernelName", commonDefinitions.getKernelName(className));
		st.add("allocation", null);
		st.add("variables", null);
		st.add("inputSize", null);
		st.add("kernels", null);
		st.add("tileSize", null);
		st.add("destinationVariable", null);
		st.add("sequentialNonFinalVariables", null);
		st.add("additionalStatements", null);
		if (operation.operationType == OperationType.Reduce) {
			fillReduceOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Foreach) {
			fillForeachOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Map) {
			fillMapOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Filter) {
			fillFilterOperationCall(st, operation, className);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		for (Variable variable : operation.getExternalVariables()) {
			if (!isSequential || variable.isFinal()) {
				st.addAggr("variables.{gVariableName, variableName}",
						getGlobalVariableName(variable, operation),
						variable.name);
			} else if (isSequential && !variable.isFinal()) {
				String allName = getOutputVariableName(variable, operation);
				st.addAggr("sequentialNonFinalVariables.{allName, arrName}",
						allName, variable.name);
			}
		}
		if (isSequential) {
			st.addAggr("allocation.{body}", this
					.createSequentialAllocationJavaFile(className, operation));
		}
		return st.render();
	}

	/**
	 * Create allocations for non-final variables in sequential operations.
	 */
	private String createSequentialAllocationJavaFile(String className,
			Operation operation) {
		ST st = new ST(templateSequentialAllocationJavaFile);
		st.add("kernelName", commonDefinitions.getKernelName(className));
		st.add("rsVarName", getRSVariableName());
		st.add("externalVariables", null);
		for (Variable variable : operation.getExternalVariables()) {
			if (!variable.isFinal()) {
				String allName = getOutputVariableName(variable, operation);
				String elementType = getRenderScriptJavaType(variable.typeName);
				String gName = getGlobalVariableName(variable, operation);
				String gNameOut = getOutputVariableName(variable, operation);
				st.addAggr(
						"externalVariables.{allName, elementType, gName, gNameOut, name}",
						allName, elementType, gName, gNameOut, variable.name);
			}
		}
		return st.render();
	}

	/**
	 * Return a string containing the size for the destination array that is
	 * used temporarily to store the processing results.
	 */
	abstract protected String getDestinationArraySize(Operation operation);

	/**
	 * Return a string for creating a new object for the operation destination
	 * variable, using data provided by content of variableName.
	 */
	abstract protected String getReturnObjectCreation(Operation operation,
			String variableName);

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid reduce operation call.
	 */
	protected void fillReduceOperationCall(ST st, Operation operation) {
		String varName = getOutputVariableName(operation.destinationVariable,
				operation);
		// For typed classes like Array, get the type parameter
		String allocationJavaType = commonDefinitions
				.isImage(operation.variable) ? Pixel.getInstance()
				.getClassName() : operation.variable.typeParameters.get(0);
		st.addAggr(
				"allocation.{body}",
				createAllocation(operation, "1", varName, allocationJavaType,
						true));
		String destVarName = getOutputVariableName(
				operation.destinationVariable, operation);
		st.addAggr("variables.{gVariableName, variableName}", destVarName,
				destVarName);
		st.addAggr("variables.{gVariableName, variableName}",
				getInputDataVariableName(operation),
				commonDefinitions.getVariableOutName(operation.variable));
		String variableAllocation = commonDefinitions
				.getVariableOutName(operation.variable);
		st.add("allocationName", variableAllocation);
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			String tileVariableName = getTileVariableName(operation);
			st.addAggr(
					"allocation.{body}",
					createAllocation(operation,
							getTileSizeVariableName(operation),
							tileVariableName, allocationJavaType, true));
			st.addAggr("kernels.{functionName, allocations, rsOperationName}",
					commonDefinitions.getOperationTileFunctionName(operation),
					tileVariableName, "forEach");
			st.addAggr("variables.{gVariableName, variableName}",
					tileVariableName, tileVariableName);
		}
		st.addAggr("kernels.{functionName, allocations, rsOperationName}",
				commonDefinitions.getOperationName(operation), "", "invoke");
		if (operation.destinationVariable != null) {
			String variableName = getOutputVariableName(
					operation.destinationVariable, operation);
			String tempVariableName = variableName + "Tmp";
			String nativeReturnType = commonDefinitions
					.translateType(operation.destinationVariable.typeName);
			String size = getDestinationArraySize(operation);
			String returnObjectCreation = getReturnObjectCreation(operation,
					tempVariableName);
			st.addAggr(
					"destinationVariable.{name, tmpName, nativeReturnType, size, returnObjectCreation}",
					variableName, tempVariableName, nativeReturnType, size,
					returnObjectCreation);
			st.addAggr("params.{name}", variableName);
		}
	}

	/**
	 * Returns an equivalent RenderScript Java type for a given user-library or
	 * Java type.
	 */
	protected String getRenderScriptJavaType(String typeName) {
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.getClass(typeName);
		if (userLibraryClass != null) {
			if (!(userLibraryClass instanceof UserLibraryDataType)) {
				throw new RuntimeException(String.format(
						"'%s' is not convertible to a RenderScript type.",
						typeName));
			}
			return ((UserLibraryDataType) userLibraryClass)
					.getRenderScriptJavaType();
		} else {
			if (!PrimitiveTypes.isPrimitive(typeName)) {
				throw new RuntimeException(
						String.format(
								"'%s' is not a valid primitive Java type or User Library "
										+ "Data Type convertible to a RenderScript type.",
								typeName));
			}
			return PrimitiveTypes.getRenderScriptJavaType(typeName);
		}
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid foreach operation call.
	 */
	private void fillForeachOperationCall(ST st, Operation operation) {
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		String variableAllocation = commonDefinitions
				.getVariableOutName(operation.variable);
		st.add("allocationName", variableAllocation);
		if (isSequential) {
			st.addAggr("variables.{gVariableName, variableName}",
					getInputDataVariableName(operation), variableAllocation);
			st.addAggr("kernels.{functionName, allocations, rsOperationName}",
					commonDefinitions.getOperationName(operation), "", "invoke");
		} else {
			st.addAggr("kernels.{functionName, allocations, rsOperationName}",
					commonDefinitions.getOperationName(operation),
					variableAllocation + ", " + variableAllocation, "forEach");
		}
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid map operation call.
	 */
	private void fillMapOperationCall(ST st, Operation operation) {
		String destVarAllocation = commonDefinitions
				.getVariableOutName(operation.destinationVariable);
		String varAllocation = commonDefinitions
				.getVariableOutName(operation.variable);
		String expression;
		if (commonDefinitions.isImage(operation.variable)) {
			expression = String.format(
					"%s.getType().getX() * %s.getType().getY()", varAllocation,
					varAllocation);
		} else {
			expression = String.format("%s.getType().getX()", varAllocation);
		}
		String allocationJavaType = operation.destinationVariable.typeParameters
				.get(0);
		st.addAggr(
				"allocation.{body}",
				createAllocation(operation, expression, destVarAllocation,
						allocationJavaType, false));
		String inAllocation = commonDefinitions
				.getVariableOutName(operation.variable);
		String outAllocation = commonDefinitions
				.getVariableOutName(operation.destinationVariable);
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		if (isSequential) {
			st.addAggr("variables.{gVariableName, variableName}",
					getInputDataVariableName(operation), inAllocation);
			st.addAggr("variables.{gVariableName, variableName}",
					getOutputDataVariableName(operation), outAllocation);
			st.addAggr("kernels.{functionName, allocations, rsOperationName}",
					commonDefinitions.getOperationName(operation), "", "invoke");
		} else {
			st.addAggr("kernels.{functionName, allocations, rsOperationName}",
					commonDefinitions.getOperationName(operation), inAllocation
							+ ", " + outAllocation, "forEach");
		}
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid filter operation call.
	 */
	private void fillFilterOperationCall(ST st, Operation operation,
			String className) {
		String tileAllocation = getOutputTileVariableName(operation);
		String varAllocation = commonDefinitions
				.getVariableOutName(operation.variable);
		String expression;
		if (commonDefinitions.isImage(operation.variable)) {
			expression = String.format(
					"%s.getType().getX() * %s.getType().getY()", varAllocation,
					varAllocation);
		} else {
			expression = String.format("%s.getType().getX()", varAllocation);
		}
		// 1. Create tile and size allocations
		st.addAggr(
				"allocation.{body}",
				createAllocation(operation, expression, tileAllocation, "int",
						true));
		String sizeAllocation = getOutputXSizeAllocationVariableName(operation);
		st.addAggr("allocation.{body}",
				createAllocation(operation, "1", sizeAllocation, "int", true));
		// 2. Set allocations
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		if (isSequential) {
			st.addAggr("variables.{gVariableName, variableName}",
					getInputDataVariableName(operation), varAllocation);
		}
		st.addAggr("variables.{gVariableName, variableName}", tileAllocation,
				tileAllocation);
		st.addAggr("variables.{gVariableName, variableName}", sizeAllocation,
				sizeAllocation);
		// 3. Call functions
		String rsOperationName = isSequential ? "invoke" : "forEach";
		String inAllocation = commonDefinitions
				.getVariableOutName(operation.variable);
		String allocations = isSequential ? "" : inAllocation + ", "
				+ tileAllocation;
		st.addAggr("kernels.{functionName, allocations, rsOperationName}",
				commonDefinitions.getOperationTileFunctionName(operation),
				allocations, rsOperationName);
		st.addAggr(
				"kernels.{functionName, allocations, rsOperationName}",
				commonDefinitions.getOperationAllocationFunctionName(operation),
				"", "invoke");
		// 4. Build if body
		ST stIfBody = new ST(templateFilterOperationCall);
		String ifSizeVar = commonDefinitions.getPrefix() + "size";
		stIfBody.add("kernelName", commonDefinitions.getKernelName(className));
		stIfBody.add("sizeVarName", ifSizeVar);
		stIfBody.add("gSizeVariableName", sizeAllocation);
		String destVarAllocation = commonDefinitions
				.getVariableOutName(operation.destinationVariable);
		stIfBody.addAggr(
				"allocation.{body}",
				createAllocation(operation, ifSizeVar + "[0]",
						destVarAllocation, getNativeReturnType(operation),
						false));
		stIfBody.addAggr("variables.{gVariableName, variableName}",
				getOutputDataVariableName(operation), destVarAllocation);
		stIfBody.addAggr("variables.{gVariableName, variableName}",
				getInputDataVariableName(operation), varAllocation);
		stIfBody.addAggr(
				"kernels.{functionName, allocations, rsOperationName}",
				commonDefinitions.getOperationName(operation), "", "invoke");
		st.addAggr("additionalStatements.{value}", stIfBody.render());
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
	 * Translates a parallel foreach or map returning a C code compatible with
	 * this runtime.
	 */
	private String translateParallelForeachMap(Operation operation) {
		ST st = new ST(templateParallelForeachMapFunction);
		st.add("userFunction",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("varName", operation.getUserFunctionData().arguments.get(0));
		st.add("isImage", null);
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("isImage", "");
		}
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	/**
	 * Translates a sequential foreach or map returning a C code compatible with
	 * this runtime.
	 */
	private String translateSequentialForeachMap(Operation operation) {
		ST st = new ST(templateSequentialFunction);
		setCommonParameters(operation, st);
		setExternalVariables(operation, st);
		if (operation.operationType == OperationType.Foreach) {
			// Types in foreach are always the same
			String type = getNativeReturnType(operation);
			st.add("body", getSetValuesInAllocations(operation, type, type));
		} else {
			// Map has different types for input (get) and output (set)
			String typeSet = getNativeReturnType(operation);
			String typeGet = commonDefinitions
					.translateType(operation.variable.typeParameters.get(0));
			st.add("body",
					getSetValuesInAllocations(operation, typeSet, typeGet));

		}
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	private String getSetValuesInAllocations(Operation operation,
			String typeSet, String typeGet) {
		ST st = new ST(templateSetValues);
		st.add("userFunction",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("typeSet", typeSet);
		st.add("typeGet", typeGet);
		setCommonParameters(operation, st);
		return st.render();
	}

	private void setCommonParameters(Operation operation, ST st) {
		st.add("readAllocation", getInputDataVariableName(operation));
		st.add("writeAllocation", getWriteAllocationName(operation));
		st.add("xVar", commonDefinitions.getPrefix() + "x");
		st.add("isImage", null);
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("isImage", "");
			st.add("yVar", commonDefinitions.getPrefix() + "y");
		}
	}

	private String getWriteAllocationName(Operation operation) {
		if (operation.operationType == OperationType.Foreach) {
			return getInputDataVariableName(operation);
		} else if (operation.operationType == OperationType.Filter) {
			return getOutputTileVariableName(operation);
		} else {
			return getOutputDataVariableName(operation);
		}
	}

	/**
	 * Return a string containing the native return type for a given operation.
	 */
	protected String getNativeReturnType(Operation operation) {
		String ret;
		if (operation.operationType == OperationType.Foreach
				|| operation.operationType == OperationType.Reduce
				|| operation.operationType == OperationType.Filter) {
			if (commonDefinitions.isImage(operation.variable)) {
				ret = commonDefinitions.translateType(operation
						.getUserFunctionData().arguments.get(0).typeName);
			} else {
				ret = commonDefinitions
						.translateType(operation.variable.typeParameters.get(0));
			}
		} else if (operation.operationType == OperationType.Map) {
			ret = commonDefinitions
					.translateType(operation.destinationVariable.typeParameters
							.get(0));
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		return ret;
	}

	/**
	 * Configure external variables in a given String Template that has the
	 * following pattern:
	 * 
	 * <setExternalVariables:{var|\t\trsSetElementAt_<var.type>(<var.
	 * allocationName>, <var.varName>, 0);\n}>
	 */
	protected void setExternalVariables(Operation operation, ST st) {
		st.add("setExternalVariables", null);
		for (Variable variable : operation.getExternalVariables()) {
			if (!variable.isFinal()) {
				st.addAggr(
						"setExternalVariables.{type, allocationName, varName}",
						commonDefinitions.translateType(variable.typeName),
						getOutputVariableName(variable, operation),
						getGlobalVariableName(variable, operation));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelReduceTile(Operation operation) {
		ST st = new ST(templateParallelReduceTile);
		st.add("xVar", commonDefinitions.getPrefix() + "x");
		Variable inputVar1 = operation.getUserFunctionData().arguments.get(0);
		Variable inputVar2 = operation.getUserFunctionData().arguments.get(1);
		st.add("inputVar1", inputVar1.name);
		st.add("inputVar2", inputVar2.name);
		// Takes the first var, since they must be the same for reduce
		// operations
		st.add("varType", commonDefinitions.translateType(inputVar1.typeName));
		st.add("userFunctionName",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("dataVar", getInputDataVariableName(operation));
		st.add("dataVarTile", getTileVariableName(operation));
		st.add("sizeVar",
				getAllocationDimCall("X", getTileVariableName(operation)));
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("declBaseVar", null);
			st.add("baseVar", "x, 0");
			st.add("x", "");
		} else {
			st.add("x", null);
			st.add("declBaseVar", "");
			st.add("baseVar", getBaseVariableName());
		}
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
		ST st = new ST(templateFilter);
		String prefix = this.commonDefinitions.getPrefix();
		st.add("varCount", prefix + "count");
		st.add("xVar", prefix + "x");
		st.add("tileAllocation", getOutputTileVariableName(operation));
		st.add("outputAllocation", getOutputDataVariableName(operation));
		st.add("inputAllocation", getInputDataVariableName(operation));
		st.add("type", commonDefinitions
				.translateType(operation.variable.typeParameters.get(0)));
		st.add("isImage", null);
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("isImage", "");
			st.add("yVar", prefix + "y");
		}
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateFilterTile(Operation operation) {
		String tileFunction;
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			tileFunction = translateParallelFilterTile(operation);
		} else {
			tileFunction = translateSequentialFilterTile(operation);
		}
		StringBuilder ret = new StringBuilder();
		ret.append(createKernelFunction(operation, tileFunction,
				FunctionType.Tile));
		// Set allocation function
		ret.append("\n\n");
		ST st = new ST(
				"\trsSetElementAt_int(<allocationValue>, <intValue>, 0);\n");
		st.add("allocationValue",
				getOutputXSizeAllocationVariableName(operation));
		st.add("intValue", getOutputXSizeVariableName(operation));
		ret.append(createKernelFunction(operation, st.render(),
				FunctionType.SetAllocation));
		return ret.toString();

	}

	private String translateParallelFilterTile(Operation operation) {
		ST st = new ST(templateParallelFilterTile);
		Variable userFunctionVariable = operation.getUserFunctionData().arguments
				.get(0);
		st.add("userFunctionName",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("varName", userFunctionVariable.name);
		st.add("varCounterName", getOutputXSizeVariableName(operation));
		return st.render();
	}

	private String translateSequentialFilterTile(Operation operation) {
		ST st = new ST(templateSequentialFunction);
		setCommonParameters(operation, st);
		setExternalVariables(operation, st);
		String templateSequentialFilterTile = "if (<userFunction>(rsGetElementAt_<type>(<inputAllocation>, <xTileVar><isImage:{var|, <yVar>}>))) {\n"
				+ "\trsSetElementAt_int(<tileAllocation>, <xTileVar>, <xTileVar>);\n"
				+ "\t<sizeVarName>++;\n"
				+ "} else {\n"
				+ "\trsSetElementAt_int(<tileAllocation>, -1, <xTileVar>);\n"
				+ "}\n" + "<xTileVar>++;";
		ST stBody = new ST(templateSequentialFilterTile);
		boolean isImage = commonDefinitions.isImage(operation.variable);
		stBody.add("isImage", isImage ? "" : null);
		stBody.add("userFunction",
				commonDefinitions.getOperationUserFunctionName(operation));
		stBody.add("type", getNativeReturnType(operation));
		String xTileVar = commonDefinitions.getPrefix() + "tileX";
		stBody.add("xTileVar", xTileVar);
		stBody.add("yVar", commonDefinitions.getPrefix() + "Y");
		stBody.add("inputAllocation", getInputDataVariableName(operation));
		stBody.add("tileAllocation", getOutputTileVariableName(operation));
		stBody.add("sizeVarName", getOutputXSizeVariableName(operation));
		st.add("body", stBody.render());
		return "\tint " + xTileVar + " = 0;\n" + st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateUserFunction(Operation operation) {
		String userCode = commonDefinitions.removeCurlyBraces(operation
				.getUserFunctionData().Code.trim());
		userCode = userCode.replaceAll("[\t]", "");
		for (Variable userFunctionVariable : operation.getUserFunctionData().arguments) {
			userCode = translateVariable(userFunctionVariable,
					cCodeTranslator.translate(userCode));
		}
		// Insert external variables as global variables
		StringBuffer externalVariables = new StringBuffer();
		for (Variable variable : operation.getExternalVariables()) {
			String gVariableName = getGlobalVariableName(variable, operation);
			externalVariables.append(variable.typeName + " " + gVariableName
					+ ";\n");
			userCode = userCode.replaceAll(variable.name, gVariableName);
		}
		if (!operation.getExternalVariables().isEmpty())
			externalVariables.append("\n");
		// Foreach operations must add a return statements.
		if (operation.operationType == OperationType.Foreach) {
			userCode += String.format("\treturn %s;\n",
					operation.getUserFunctionData().arguments.get(0));
		}
		return createAllocationRSFile(operation)
				+ createKernelFunction(operation, userCode,
						FunctionType.UserCode);
	}

	private String createAllocationRSFile(Operation operation) {
		ST st = new ST(templateAllocationRSFile);
		st.add("sizeVar", null);
		st.add("allocation", null);
		if (operation.operationType == OperationType.Foreach) {
			fillAllocationForeach(operation, st);
		} else if (operation.operationType == OperationType.Reduce) {
			fillAllocationReduce(operation, st);
		} else if (operation.operationType == OperationType.Filter) {
			fillAllocationFilter(operation, st);
		} else if (operation.operationType == OperationType.Map) {
			fillAllocationMap(operation, st);
		}
		st.add("externalVariables", null);
		for (Variable variable : operation.getExternalVariables()) {
			String gNameOut = getOutputVariableName(variable, operation);
			if (!variable.isFinal())
				st.addAggr("allocation.{name}", gNameOut);
			String gNameVar = getGlobalVariableName(variable, operation);
			st.addAggr("externalVariables.{type, name}", variable.typeName,
					gNameVar);
		}
		return st.render();
	}

	private void fillAllocationForeach(Operation operation, ST st) {
		if (operation.getExecutionType() == ExecutionType.Sequential) {
			st.addAggr("allocation.{name}", getInputDataVariableName(operation));
		}
	}

	private void fillAllocationReduce(Operation operation, ST st) {
		st.addAggr("allocation.{name}", getInputDataVariableName(operation));
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			st.addAggr("allocation.{name}", getTileVariableName(operation));
		}
		st.addAggr("allocation.{name}",
				getOutputVariableName(operation.destinationVariable, operation));
	}

	private void fillAllocationMap(Operation operation, ST st) {
		if (operation.getExecutionType() == ExecutionType.Sequential) {
			st.addAggr("allocation.{name}", getInputDataVariableName(operation));
			st.addAggr("allocation.{name}",
					getOutputDataVariableName(operation));
		}
	}

	private void fillAllocationFilter(Operation operation, ST st) {
		st.addAggr("allocation.{name}", getInputDataVariableName(operation));
		st.addAggr("allocation.{name}", getOutputDataVariableName(operation));
		st.addAggr("allocation.{name}",
				getOutputXSizeAllocationVariableName(operation));
		st.addAggr("allocation.{name}", getOutputTileVariableName(operation));
		st.addAggr("sizeVar.{name}", getOutputXSizeVariableName(operation));
	}

	/**
	 * Create a function signature for a given operation.
	 */
	@Override
	protected String getOperationFunctionSignature(Operation operation,
			FunctionType functionType) {
		String ret;
		if (functionType == FunctionType.UserCode) {
			ret = initializeUserFunctionSignature(operation);
		} else if (operation.operationType == OperationType.Foreach) {
			ret = initializeForeachSignature(operation, functionType);
		} else if (operation.operationType == OperationType.Reduce) {
			ret = initializeReduceSignature(operation, functionType);
		} else if (operation.operationType == OperationType.Map) {
			ret = initializeMapSignature(operation, functionType);
		} else if (operation.operationType == OperationType.Filter) {
			ret = initializeFilterSignature(operation, functionType);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		return ret;
	}

	/**
	 * Initialize the user function signature.
	 */
	private String initializeUserFunctionSignature(Operation operation) {
		ST st = new ST(templateFunctionDecl);
		st.add("functionName",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("returnType", getFunctionReturnType(operation));
		st.addAggr("modifier.{value}", "static");
		st.add("isKernel", null);
		st.add("params", null);
		for (Variable param : operation.getUserFunctionData().arguments) {
			String paramType = commonDefinitions.translateType(param.typeName);
			st.addAggr("params.{type, name}", paramType, param.name);
		}
		return st.render();
	}

	/**
	 * Get the equivalent C return type for each operation function type.
	 */
	private String getFunctionReturnType(Operation operation) {
		String returnType;
		if (operation.operationType == OperationType.Foreach
				|| operation.operationType == OperationType.Reduce) {
			returnType = commonDefinitions.translateType(operation
					.getUserFunctionData().arguments.get(0).typeName);
		} else if (operation.operationType == OperationType.Filter) {
			returnType = "bool";
		} else if (operation.operationType == OperationType.Map) {
			returnType = commonDefinitions
					.translateType(operation.destinationVariable.typeParameters
							.get(0));
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		return returnType;
	}

	/**
	 * Initialize foreach function signature.
	 */
	private String initializeForeachSignature(Operation operation,
			FunctionType functionType) {
		String returnType = commonDefinitions.translateType(operation
				.getUserFunctionData().arguments.get(0).typeName);
		return initializeSingleParameterFunction(operation, returnType,
				commonDefinitions.getOperationName(operation)).render();
	}

	/**
	 * Initialize a generic single parameter function signature.
	 */
	private ST initializeSingleParameterFunction(Operation operation,
			String returnType, String functionName) {
		ST st = new ST(templateFunctionDecl);
		st.add("modifier", null);
		st.add("functionName", functionName);
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			st.add("returnType", returnType);
			st.add("isKernel", "");
			Variable inputVar1 = operation.getUserFunctionData().arguments
					.get(0);
			String inputVar1Type = commonDefinitions
					.translateType(inputVar1.typeName);
			st.addAggr("params.{type, name}", inputVar1Type, inputVar1.name);
			st.addAggr("params.{type, name}", "uint32_t", "x");
			st.addAggr("params.{type, name}", "uint32_t", "y");
		} else {
			st.add("returnType", "void");
			st.add("isKernel", null);
			st.add("params", null);
		}
		return st;
	}

	/**
	 * Initialize reduce function signature.
	 */
	private String initializeReduceSignature(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		st.add("modifier", null);
		st.add("isKernel", null);
		st.add("params", null);
		String returnType = commonDefinitions.translateType(operation
				.getUserFunctionData().arguments.get(0).typeName);
		if (functionType == FunctionType.BaseOperation) {
			st.add("returnType", "void");
			st.add("functionName",
					commonDefinitions.getOperationName(operation));
		} else {
			// Tile function type
			st.add("returnType", returnType);
			st.add("isKernel", "");
			st.addAggr("params.{type, name}", "uint32_t", "x");
			st.add("functionName",
					commonDefinitions.getOperationTileFunctionName(operation));
		}
		return st.render();
	}

	/**
	 * Initialize map function signature.
	 */
	private String initializeMapSignature(Operation operation,
			FunctionType functionType) {
		String returnType = commonDefinitions
				.translateType(operation.destinationVariable.typeParameters
						.get(0));
		return initializeSingleParameterFunction(operation, returnType,
				commonDefinitions.getOperationName(operation)).render();
	}

	/**
	 * Initialize filter function signature.
	 */
	private String initializeFilterSignature(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		st.add("modifier", null);
		st.add("isKernel", null);
		st.add("params", null);
		String returnType = commonDefinitions.translateType(operation
				.getUserFunctionData().arguments.get(0).typeName);
		if (functionType == FunctionType.BaseOperation) {
			st.add("returnType", "void");
			st.add("functionName",
					commonDefinitions.getOperationName(operation));
		} else if (functionType == FunctionType.Tile) {
			st = initializeSingleParameterFunction(operation, returnType,
					commonDefinitions.getOperationTileFunctionName(operation));
		} else {
			// SetAllocation function type
			st.add("returnType", "void");
			st.add("functionName", commonDefinitions
					.getOperationAllocationFunctionName(operation));
		}
		return st.render();
	}

	/**
	 * Generic function for creating allocations in Java files.
	 */
	private String createAllocation(Operation operation, String expression,
			String allocationName, String javaType, boolean declareAllocation) {
		ST st = new ST(templateAllocation);
		st.add("rsVarName", getRSVariableName());
		st.add("rsType", getRenderScriptJavaType(javaType));
		st.add("XYZ", "X");
		st.add("expression", expression);
		st.add("allocationName", allocationName);
		st.add("declareAllocation", declareAllocation ? "" : null);
		return st.render();
	}

	/**
	 * Create a global variable name for the given variable following some
	 * standards. Global variables will be prefixed with "g" followed by an
	 * upper case letter and sufixed by the operation name, so "max" from
	 * foreach 2 becomes "gMax_Foreach2"
	 */
	protected String getGlobalVariableName(String variableName,
			Operation operation) {
		String operationName = commonDefinitions.getOperationName(operation);
		String globalVariableName = upperCaseFirstLetter(variableName);
		return commonDefinitions.getPrefix() + "g" + globalVariableName
				+ upperCaseFirstLetter(operationName);
	}

	protected String getGlobalVariableName(Variable variable,
			Operation operation) {
		return getGlobalVariableName(variable.name, operation);
	}

	/**
	 * Change the first letter of the informed string to upper case.
	 */
	protected String upperCaseFirstLetter(String string) {
		return string.substring(0, 1).toUpperCase()
				+ string.substring(1, string.length());
	}

	protected String getTileVariableName(Operation operation) {
		return getGlobalVariableName("Tile", operation);
	}

	protected String getTileVariableName(Variable variable, Operation operation) {
		return getGlobalVariableName(variable.name + "Tile", operation);
	}

	protected String getTileSizeVariableName(Operation operation) {
		return getGlobalVariableName("TileSize", operation);
	}

	protected String getInputXSizeVariableName(Operation operation) {
		return getGlobalVariableName("InputXSize", operation);
	}

	protected String getOutputXSizeVariableName(Operation operation) {
		return getGlobalVariableName("OutputXSize", operation);
	}

	protected String getOutputXSizeAllocationVariableName(Operation operation) {
		return getGlobalVariableName("OutputXSize", operation) + "_Allocation";
	}

	protected String getInputYSizeVariableName(Operation operation) {
		return getGlobalVariableName("InputYSize", operation);
	}

	protected String getInputDataVariableName(Operation operation) {
		return getGlobalVariableName("Input", operation);
	}

	protected String getOutputDataVariableName(Operation operation) {
		return getGlobalVariableName("Output", operation);
	}

	protected String getOutputTileVariableName(Operation operation) {
		return getGlobalVariableName("OutputTile", operation);
	}

	protected String getRSVariableName() {
		return commonDefinitions.getPrefix() + "mRS";
	}

	protected String getOutputVariableName(Variable variable,
			Operation operation) {
		return getGlobalVariableName("output"
				+ upperCaseFirstLetter(variable.name), operation);
	}

	protected String getAllocationDimCall(String axisLabel,
			String allocationName) {
		return String.format("rsAllocationGetDim%s(%s)", axisLabel,
				allocationName);
	}
}
