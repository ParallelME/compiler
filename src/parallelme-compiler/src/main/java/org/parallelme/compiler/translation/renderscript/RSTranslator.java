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
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.Float32;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.parallelme.compiler.userlibrary.classes.Int16;
import org.parallelme.compiler.userlibrary.classes.Int32;
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
			+ "<destinationVariable:{var|\n\n<var.nativeReturnType>[] <var.tmpName> = new <var.nativeReturnType>[<var.size>];\n"
			+ "<var.name>.copyTo(<var.tmpName>);\n"
			+ "return new <var.returnObjectCreation>;}>";
	private static final String templateAllocationRSFile = "<allocation:{var|rs_allocation <var.name>;\n}>"
			+ "<sizeVar:{var|int <var.name>;\n}>"
			+ "<externalVariables:{var|<var.variableType> <var.variableName>;\n}>\n";
	private static final String templateSequentialAllocationJavaFile = "<externalVariables:{var|"
			+ "Allocation <var.allName> = Allocation.createSized(<rsVarName>, Element.<var.elementType>(<rsVarName>), 1);\n"
			+ "<kernelName>.set_<var.gName>(<var.name>[0]);\n"
			+ "<kernelName>.set_<var.gNameOut>(<var.allName>);}; separator=\"\\n\">";
	private static final String templateSequentialForeach = "<allocation>"
			+ "<functionSignature>\n {\n"
			+ "\t<userFunctionVarType> <userFunctionVarName>;\n"
			+ "\t<forLoop>"
			+ "\t<externalVariables:{var|rsSetElementAt_<var.variableType>(<var.outVariableName>, <var.variableName>, 0);\n}>"
			+ "}";
	protected static final String templateReduceForBody = "<inputVar2> = rsGetElementAt_<varType>(<dataVar>, <xVar><yVar:{var|, <var.name>}>);\n"
			+ "<inputVar1> = <userFunctionName>(<inputVar1>, <inputVar2>);\n";
	protected static final String templateForLoop = "for (int <varName>=<initValue>; <varName> \\< <varMaxVal>; ++<varName>) {\n\t<body>}\n";
	protected static final String templateReduce = "\t<varType> <inputVar1> = rsGetElementAt_<varType>(<dataVar>, 0);\n"
			+ "\t<varType> <inputVar2>;\n"
			+ "\t<forLoop:{var|<var.loop>}>"
			+ "\trsSetElementAt_<varType>(<destVar>, param1, 0);";
	private static final String templateParallelReduceTile = "<declBaseVar:{var|\tint <baseVar> = x * <sizeVar>;\n}>"
			+ "\t<varType> <inputVar1> = rsGetElementAt_<varType>(<dataVar>, <baseVar>);\n"
			+ "\t<varType> <inputVar2>;\n"
			+ "\tfor (int <xVar>=1; <xVar>\\<<sizeVar>; ++<xVar>) {\n"
			+ "\t\t<inputVar2> = rsGetElementAt_<varType>(<dataVar>, <x:{var|x, }><declBaseVar:{var|<baseVar> + }><xVar>);\n"
			+ "\t\t<inputVar1> = <userFunctionName>(<inputVar1>, <inputVar2>);\n"
			+ "\t}\n" + "\treturn param1;\n";
	private static final String templateFilter = "\tint <varCount> = 0;\n"
			+ "\tfor (int <xVar>=0; <xVar>\\<rsAllocationGetDimX(<tileAllocation>); ++<xVar>) {\n"
			+ "<y:{var|\tfor (int <yVar>=0; <yVar>\\<rsAllocationGetDimY(<tileAllocation>); ++<yVar>) {\n}>"
			+ "\t\tint PM_value = rsGetElementAt_int(<tileAllocation>, <xVar><y:{var|, <yVar>}>);\n"
			+ "\t\tif (PM_value > 0) {\n"
			+ "\t\t\trsSetElementAt_<type>(<outputAllocation>, rsGetElementAt_<type>(<inputAllocation>, PM_value), <varCount>++);\n"
			+ "\t\t}\n" + "<y:{var|\t\\}\n}>" + "\t}\n";
	private static final String templateParallelFilterTile = "\tif (<userFunctionName>(<varName>)) {\n"
			+ "\t\trsAtomicInc(&<varCounterName>);\n"
			+ "\t\treturn x;\n"
			+ "\t} else {\n" + "\t\treturn -1;\n" + "\t}\n";
	private static final String templateSequentialForLoopBody = "<userFunctionVarName> = rsGetElementAt_<userFunctionVarType>(<inputData>, PM_x<param:{var|, <var.name>}>);\n"
			+ "<userCode>\n"
			+ "rsSetElementAt_<userFunctionVarType>(<inputData>, <userFunctionVarName>, PM_x<param:{var|, <var.name>}>);\n";
	private static final String templateFunctionDecl = "<modifier:{var|<var.value> }><returnType><isKernel:{var|  __attribute__((kernel))}> <functionName>("
			+ "<params:{var|<var.type> <var.name>}; separator=\", \">)";
	private static final String templateAllocation = "Type <typeName>Type = new Type.Builder(<rsVarName>, Element.<rsType>(<rsVarName>))\n"
			+ "\t.set<XYZ>(<expression>)\n"
			+ "\t.create();\n"
			+ "<declareAllocation:{var|Allocation }><varAllocationName> = Allocation.createTyped(<rsVarName>, <typeName>Type);";

	// Keeps a key-value map of equivalent types from Java to RenderScript
	// allocation.
	protected static Map<String, String> java2RSAllocationTypes = null;
	protected CTranslator cCodeTranslator;

	public RSTranslator(CTranslator cCodeTranslator) {
		this.cCodeTranslator = cCodeTranslator;
		if (java2RSAllocationTypes == null)
			initJava2RSAllocationTypes();
	}

	private void initJava2RSAllocationTypes() {
		java2RSAllocationTypes = new HashMap<>();
		java2RSAllocationTypes.put(Int16.getInstance().getClassName(), "I16");
		java2RSAllocationTypes.put(Int32.getInstance().getClassName(), "I32");
		java2RSAllocationTypes.put(Float32.getInstance().getClassName(), "F32");
		java2RSAllocationTypes.put(BitmapImage.getInstance().getClassName(),
				"F32_3");
		java2RSAllocationTypes.put(HDRImage.getInstance().getClassName(),
				"F32_4");
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
		if (operation.operationType == OperationType.Reduce) {
			fillReduceOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Foreach) {
			fillForeachOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Map) {
			fillMapOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Filter) {
			fillFilterOperationCall(st, operation);
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
				String allName = getAllocationName(variable.name, operation);
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
				String allName = getAllocationName(variable.name, operation);
				String elementType = java2RSAllocationTypes
						.get(variable.typeName);
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
	 * Return a string containing the native return type for a given operation.
	 */
	protected String getNativeReturnType(Operation operation) {
		String ret;
		if (operation.operationType == OperationType.Reduce) {
			if (commonDefinitions.isImage(operation.variable)) {
				ret = commonDefinitions
						.translateType(operation.variable.typeName);
			} else {
				ret = commonDefinitions
						.translateType(operation.variable.typeParameters.get(0));
			}
		} else if (operation.operationType == OperationType.Map
				|| operation.operationType == OperationType.Filter) {
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
		st.addAggr("allocation.{body}", createReturnAllocation(operation));
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
		st.addAggr("inputSize.{name, XYZ, allocationName}",
				getInputXSizeVariableName(operation), "X", variableAllocation);
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			String tileSizeVariableName = getTileSizeVariableName(operation);
			st.addAggr("allocation.{body}", createTileAllocation(operation));
			String tileVariableName = getTileVariableName(operation);
			st.addAggr("kernels.{functionName, allocations, rsOperationName}",
					commonDefinitions.getOperationTileFunctionName(operation),
					tileVariableName, "forEach");
			st.addAggr("variables.{gVariableName, variableName}",
					tileVariableName, tileVariableName);
			st.addAggr("variables.{gVariableName, variableName}",
					tileSizeVariableName, tileSizeVariableName);
		}
		st.addAggr("kernels.{functionName, allocations, rsOperationName}",
				commonDefinitions.getOperationName(operation), "", "invoke");
		if (operation.destinationVariable != null) {
			String variableName = getOutputVariableName(
					operation.destinationVariable, operation);
			String tempVariableName = variableName + "Tmp";
			String nativeReturnType = getNativeReturnType(operation);
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

	protected String createTileAllocation(Operation operation) {
		ST st = new ST(templateAllocation);
		// For typed classes like Array, get the type parameter
		String type = operation.variable.typeParameters == null
				|| operation.variable.typeParameters.isEmpty() ? operation.variable.typeName
				: operation.variable.typeParameters.get(0);
		String tileVariableName = getTileVariableName(operation);
		st.add("typeName", tileVariableName);
		st.add("rsVarName", getRSVariableName());
		st.add("rsType", java2RSAllocationTypes.get(type));
		st.add("XYZ", "X");
		st.add("expression", getTileSizeVariableName(operation));
		st.add("varAllocationName", tileVariableName);
		st.add("declareAllocation", "");
		return st.render();
	}

	protected String createReturnAllocation(Operation operation) {
		ST st = new ST(templateAllocation);
		// For typed classes like Array, get the type parameter
		String type = operation.variable.typeParameters == null
				|| operation.variable.typeParameters.isEmpty() ? operation.variable.typeName
				: operation.variable.typeParameters.get(0);
		String varName = getOutputVariableName(operation.destinationVariable,
				operation);
		st.add("typeName", varName);
		st.add("rsVarName", getRSVariableName());
		st.add("rsType", java2RSAllocationTypes.get(type));
		st.add("XYZ", "X");
		st.add("expression", "1");
		st.add("varAllocationName", varName);
		st.add("declareAllocation", "");
		return st.render();
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
			st.addAggr("inputSize.{name, XYZ, allocationName}",
					getInputXSizeVariableName(operation), "X",
					variableAllocation);
			if (commonDefinitions.isImage(operation.variable)) {
				st.addAggr("inputSize.{name, XYZ, allocationName}",
						getInputYSizeVariableName(operation), "Y",
						variableAllocation);
			}
			st.addAggr("variables.{gVariableName, variableName}",
					getInputDataVariableName(operation), variableAllocation);
		}
		String rsOperationName = isSequential ? "invoke" : "forEach";
		st.addAggr("kernels.{functionName, allocations, rsOperationName}",
				commonDefinitions.getOperationName(operation),
				variableAllocation + ", " + variableAllocation, rsOperationName);
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid map operation call.
	 */
	private void fillMapOperationCall(ST st, Operation operation) {
		st.addAggr("allocation.{body}", createMapAllocation(operation));
		String inAllocation = commonDefinitions
				.getVariableOutName(operation.variable);
		String outAllocation = commonDefinitions
				.getVariableOutName(operation.destinationVariable);
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		String rsOperationName = isSequential ? "invoke" : "forEach";
		st.addAggr("kernels.{functionName, allocations, rsOperationName}",
				commonDefinitions.getOperationName(operation), inAllocation
						+ ", " + outAllocation, rsOperationName);
	}

	private String createMapAllocation(Operation operation) {
		ST st = new ST(templateAllocation);
		// For typed classes like Array, get the type parameter
		String type = operation.destinationVariable.typeParameters.get(0);
		String destVarAllocation = commonDefinitions
				.getVariableOutName(operation.destinationVariable);
		String varAllocation = commonDefinitions
				.getVariableOutName(operation.variable);
		st.add("typeName", destVarAllocation);
		st.add("rsVarName", getRSVariableName());
		st.add("rsType", java2RSAllocationTypes.get(type));
		st.add("XYZ", "X");
		String expression;
		if (commonDefinitions.isImage(operation.variable)) {
			expression = String.format(
					"%s.getType().getX() * %s.getType().getY()", varAllocation,
					varAllocation);
		} else {
			expression = String.format("%s.getType().getX()", varAllocation);
		}
		st.add("expression", expression);
		st.add("varAllocationName", destVarAllocation);
		st.add("declareAllocation", null);
		return st.render();
	}

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid filter operation call.
	 */
	private void fillFilterOperationCall(ST st, Operation operation) {
		fillMapOperationCall(st, operation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateForeach(Operation operation) {
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			return translateParallelForeach(operation);
		} else {
			return translateSequentialForeach(operation);
		}
	}

	/**
	 * Translates a parallel foreach operation returning a C code compatible
	 * with this runtime.
	 */
	private String translateParallelForeach(Operation operation) {
		return translateParallelOperation(operation, true);
	}

	private String translateParallelOperation(Operation operation,
			boolean addReturnStatement) {
		Variable userFunctionVariable = operation.getUserFunctionData().arguments
				.get(0);
		String code2Translate = operation.getUserFunctionData().Code.trim();
		String ret;
		if (addReturnStatement) {
			// Remove the last curly brace
			code2Translate = code2Translate.substring(0,
					code2Translate.lastIndexOf("}"));
			String returnString = String.format("\treturn %s;",
					userFunctionVariable.name);
			code2Translate = code2Translate + "\n" + returnString + "\n}";
		}
		// Insert external variables as global variables
		StringBuffer externalVariables = new StringBuffer();
		for (Variable variable : operation.getExternalVariables()) {
			String gVariableName = getGlobalVariableName(variable, operation);
			externalVariables.append(variable.typeName + " " + gVariableName
					+ ";\n");
			code2Translate = code2Translate.replaceAll(variable.name,
					gVariableName);
		}
		if (!operation.getExternalVariables().isEmpty())
			externalVariables.append("\n");
		ret = externalVariables.toString()
				+ getOperationFunctionSignature(operation,
						FunctionType.BaseOperation)
				+ translateVariable(userFunctionVariable,
						cCodeTranslator.translate(code2Translate));
		return ret;
	}

	/**
	 * Translates a sequential foreach operation returning a C code compatible
	 * with this runtime.
	 */
	private String translateSequentialForeach(Operation operation) {
		Variable userFunctionVariable = operation.getUserFunctionData().arguments
				.get(0);
		String code2Translate = operation.getUserFunctionData().Code.trim();
		// Remove the last curly brace
		code2Translate = code2Translate.substring(0,
				code2Translate.lastIndexOf("}"));
		// Remove the first curly brace
		code2Translate = code2Translate.substring(
				code2Translate.indexOf("{") + 1, code2Translate.length());
		ST st = new ST(templateSequentialForeach);
		st.add("functionSignature",
				getOperationFunctionSignature(operation,
						FunctionType.BaseOperation));
		String userFunctionVarType = commonDefinitions
				.translateType(userFunctionVariable.typeName);
		st.add("userFunctionVarName", userFunctionVariable.name);
		st.add("userFunctionVarType", userFunctionVarType);
		String cCode = translateVariable(userFunctionVariable,
				cCodeTranslator.translate(code2Translate)).trim();
		st.add("allocation", createAllocationRSFile(operation));
		st.add("externalVariables", null);
		// Must replace all user variables names in code by those new global
		// variables names
		for (Variable variable : operation.getExternalVariables()) {
			String gNameVar = getGlobalVariableName(variable, operation);
			if (!variable.isFinal()) {
				String gNameOut = getOutputVariableName(variable, operation);
				st.addAggr(
						"externalVariables.{ variableType, outVariableName, variableName }",
						variable.typeName, gNameOut, gNameVar);
			}
			cCode = cCode.replaceAll(variable.name, gNameVar);
		}
		String prefix = commonDefinitions.getPrefix();
		ST stFor = new ST(templateForLoop);
		stFor.add("varName", prefix + "x");
		stFor.add("initValue", "0");
		stFor.add("varMaxVal", getInputXSizeVariableName(operation));
		ST stForBody = new ST(templateSequentialForLoopBody);
		stForBody.add("inputData", getInputDataVariableName(operation));
		stForBody.add("userFunctionVarName", userFunctionVariable.name);
		stForBody.add("userFunctionVarType", userFunctionVarType);
		stForBody.add("userCode", cCode);
		stForBody.add("param", null);
		// Image types contains two for loops
		if (commonDefinitions.isImage(operation.variable)) {
			stForBody.addAggr("param.{name}", prefix + "y");
			ST stFor2 = new ST(templateForLoop);
			stFor2.add("initValue", "0");
			stFor2.add("varName", prefix + "y");
			stFor2.add("varMaxVal", getInputYSizeVariableName(operation));
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
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("sizeVar", getInputYSizeVariableName(operation));
			st.add("declBaseVar", null);
			st.add("baseVar", "x, 0");
			st.add("x", "");
		} else {
			st.add("sizeVar", getTileSizeVariableName(operation));
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
		return translateParallelOperation(operation, false);
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
		st.add("y", null);
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("y", "");
			st.add("yVar", prefix + "y");
		}
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelFilterTile(Operation operation) {
		ST st = new ST(templateParallelFilterTile);
		Variable userFunctionVariable = operation.getUserFunctionData().arguments
				.get(0);
		st.add("userFunctionName",
				commonDefinitions.getOperationUserFunctionName(operation));
		st.add("varName", userFunctionVariable.name);
		st.add("varCounterName", getOutputXSizeVariableName(operation));
		StringBuilder ret = new StringBuilder();
		ret.append(createKernelFunction(operation, st.render(),
				FunctionType.Tile));
		ret.append("\n\n");
		st = new ST("\trsSetElementAt_int(<allocationValue>, <intValue>, 0);\n");
		st.add("allocationValue",
				getOutputXSizeAllocationVariableName(operation));
		st.add("intValue", getOutputXSizeVariableName(operation));
		ret.append(createKernelFunction(operation, st.render(),
				FunctionType.SetAllocation));
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateUserFunction(Operation operation) {
		String userCode = commonDefinitions.removeCurlyBraces(operation
				.getUserFunctionData().Code.trim());
		for (Variable userFunctionVariable : operation.getUserFunctionData().arguments) {
			userCode = translateVariable(userFunctionVariable,
					cCodeTranslator.translate(userCode));
		}
		return createAllocationRSFile(operation)
				+ createKernelFunction(operation, userCode,
						FunctionType.UserCode);
	}

	private String createAllocationRSFile(Operation operation) {
		ST st = new ST(templateAllocationRSFile);
		st.addAggr("allocation.{name}", getInputDataVariableName(operation));
		if (operation.operationType == OperationType.Foreach
				|| operation.operationType == OperationType.Reduce) {
			String inputXSize = getInputXSizeVariableName(operation);
			st.addAggr("sizeVar.{name}", inputXSize);
			if (commonDefinitions.isImage(operation.variable)) {
				String inputYSize = getInputYSizeVariableName(operation);
				st.addAggr("sizeVar.{name}", inputYSize);
			}
		}
		if (operation.operationType == OperationType.Reduce) {
			if (operation.getExecutionType() == ExecutionType.Parallel) {
				st.addAggr("allocation.{name}", getTileVariableName(operation));
				st.addAggr("sizeVar.{name}", getTileSizeVariableName(operation));
			}
			if (operation.destinationVariable != null) {
				st.addAggr(
						"allocation.{name}",
						getOutputVariableName(operation.destinationVariable,
								operation));
			}
		}
		if (operation.operationType == OperationType.Filter
				&& operation.getExecutionType() == ExecutionType.Parallel) {
			st.addAggr("allocation.{name}",
					getOutputDataVariableName(operation));
			st.addAggr("allocation.{name}",
					getOutputXSizeAllocationVariableName(operation));
			st.addAggr("allocation.{name}",
					getOutputTileVariableName(operation));
			st.addAggr("sizeVar.{name}", getOutputXSizeVariableName(operation));
		}
		st.add("externalVariables", null);
		for (Variable variable : operation.getExternalVariables()) {
			String gNameOut = getOutputVariableName(variable, operation);
			if (!variable.isFinal())
				st.addAggr("allocation.{name}", gNameOut);
			String gNameVar = getGlobalVariableName(variable, operation);
			st.addAggr(
					"externalVariables.{ variableType, outVariableName, variableName }",
					variable.typeName, gNameOut, gNameVar);
		}
		return st.render();
	}

	/**
	 * Create a function signature for a given operation.
	 */
	@Override
	protected String getOperationFunctionSignature(Operation operation,
			FunctionType functionType) {
		ST st;
		if (operation.operationType == OperationType.Foreach) {
			st = initializeForeachSignatureTemplate(operation, functionType);
		} else if (operation.operationType == OperationType.Reduce) {
			st = initializeReduceSignatureTemplate(operation, functionType);
		} else if (operation.operationType == OperationType.Map) {
			st = initializeMapSignatureTemplate(operation, functionType);
		} else if (operation.operationType == OperationType.Filter) {
			st = initializeFilterSignatureTemplate(operation, functionType);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		return st.render();
	}

	private ST initializeForeachSignatureTemplate(Operation operation,
			FunctionType functionType) {
		String returnType = commonDefinitions.translateType(operation
				.getUserFunctionData().arguments.get(0).typeName);
		return initializeSingleParameterFunction(operation, returnType,
				commonDefinitions.getOperationName(operation));
	}

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

	private ST initializeReduceSignatureTemplate(Operation operation,
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
			st.add("returnType", returnType);
			st.add("isKernel", "");
			st.addAggr("params.{type, name}", "uint32_t", "x");
			st.add("functionName",
					commonDefinitions.getOperationTileFunctionName(operation));
		} else {
			initializeUserFunctionSignature(st, operation, returnType);
		}
		return st;
	}

	private void initializeUserFunctionSignature(ST st, Operation operation,
			String returnType) {
		st.add("returnType", returnType);
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			st.addAggr("modifier.{value}", "static");
		}
		for (Variable inputVar : operation.getUserFunctionData().arguments)
			st.addAggr("params.{type, name}",
					commonDefinitions.translateType(inputVar.typeName),
					inputVar.name);
		st.add("functionName",
				commonDefinitions.getOperationUserFunctionName(operation));
	}

	private ST initializeMapSignatureTemplate(Operation operation,
			FunctionType functionType) {
		String returnType = commonDefinitions
				.translateType(operation.destinationVariable.typeParameters
						.get(0));
		return initializeSingleParameterFunction(operation, returnType,
				commonDefinitions.getOperationName(operation));
	}

	private ST initializeFilterSignatureTemplate(Operation operation,
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
		} else if (functionType == FunctionType.UserCode) {
			initializeUserFunctionSignature(st, operation, "bool");
		} else {
			st.add("returnType", "void");
			st.add("functionName", commonDefinitions
					.getOperationAllocationFunctionName(operation));
		}
		return st;
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
		return getGlobalVariableName("OutputXSizeAllocation", operation);
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

	protected String getAllocationName(String variableName, Operation operation) {
		return getGlobalVariableName(variableName, operation) + "_Allocation";
	}
}
