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
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Base class for RenderScript translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSTranslator extends BaseUserLibraryTranslator {
	private static final String templateOperationCall = "<allocation:{var|<var.body>}; separator=\"\\n\">"
			+ "<variables:{var|\n\n<kernelName>.set_<var.gVariableName>(<var.variableName>);}>"
			+ "<inputSize:{var|\n\n<kernelName>.set_<var.name>(<allocationName>.getType().get<var.XYZ>());}>"
			+ "<kernels:{var|\n\n<kernelName>.<rsOperationName>_<var.functionName>(<var.allocations>);}>"
			+ "<sequentialNonFinalVariables:{var|\n\n<var.allName>.copyTo(<var.arrName>);}>"
			+ "<destinationVariable:{var|\n\n<var.nativeReturnType>[] <var.tmpName> = new <var.nativeReturnType>[<var.size>];\n"
			+ "<var.name>.copyTo(<var.tmpName>);\n"
			+ "return new <var.returnObjectCreation>;}>";
	private static final String templateSequentialAllocationRSFile = "rs_allocation <inputData>;\n"
			+ "<outVariable:{var|rs_allocation <var.name>;\n}>"
			+ "int <inputXSize>;\n"
			+ "int <inputYSize>;\n"
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
	private static final String templateParallelReduce = "\t<varType> <inputVar1> = rsGetElementAt_<varType>(<dataVar>, 0);\n"
			+ "\t<varType> <inputVar2>;\n"
			+ "\tfor (int i=1; i\\<<sizeVar>; ++i) {\n"
			+ "\t\t<inputVar2> = rsGetElementAt_<varType>(<dataVar>, i);\n"
			+ "\t\t<inputVar1> = <userFunctionName>(<inputVar1>, <inputVar2>);\n"
			+ "\t}\n" + "\treturn <inputVar1>;\n";
	private static final String templateParallelReduceTile = "\t<varType> <inputVar1> = rsGetElementAt_<varType>(<dataVar>, x, 0);\n"
			+ "\t<varType> <inputVar2>;\n"
			+ "\tfor (int i=1; i\\<<sizeVar>; ++i) {\n"
			+ "\t\t<inputVar2> = rsGetElementAt_<varType>(<dataVar>, x, i);\n"
			+ "\t\t<inputVar1> = <userFunctionName>(<inputVar1>, <inputVar2>);\n"
			+ "\t}\n" + "\treturn <inputVar1>;\n";

	private static final String templateForLoop = "for (int <varName> = 0; <varName> \\< <varMaxVal>; <varName>++) {\n\t<body>}\n";
	private static final String templateSequentialForLoopBody = "<userFunctionVarName> = rsGetElementAt_<userFunctionVarType>(<inputData>, PM_x<param:{var|, <var.name>}>);\n"
			+ "<userCode>\n"
			+ "rsSetElementAt_<userFunctionVarType>(<inputData>, <userFunctionVarName>, PM_x<param:{var|, <var.name>}>);\n";
	private static final String templateFunctionDecl = "<modifier:{var|<var.value> }><returnType><isKernel:{var|  __attribute__((kernel))}> <functionName>("
			+ "<params:{var|<var.type> <var.name>}; separator=\", \">)";

	// Keeps a key-value map of equivalent types from Java to RenderScript
	// allocation.
	protected static Map<String, String> java2RSAllocationTypes = null;
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
		String variableAllocation = this.commonDefinitions
				.getVariableOutName(operation.variable);
		st.add("allocationName", variableAllocation);
		st.add("rsVarName", this.getRSVariableName());
		st.add("kernelName", this.commonDefinitions.getKernelName(className));
		st.add("allocation", null);
		st.add("variables", null);
		st.add("inputSize", null);
		st.add("kernels", null);
		st.add("destinationVariable", null);
		st.add("sequentialNonFinalVariables", null);
		if (operation.getExecutionType() == ExecutionType.Parallel)
			st.add("rsOperationName", "forEach");
		else
			st.add("rsOperationName", "invoke");
		if (operation.operationType == OperationType.Reduce) {
			this.fillReduceOperationCall(st, operation);
		} else if (operation.operationType == OperationType.Foreach) {
			this.fillForeachOperationCall(st, operation);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		if (operation.destinationVariable != null) {
			String variableName = this.commonDefinitions.getPrefix()
					+ operation.destinationVariable.name;
			String tempVariableName = variableName + "Tmp";
			String nativeReturnType = this.getNativeReturnType(operation);
			String size = this.getDestinationArraySize(operation);
			String returnObjectCreation = this.getReturnObjectCreation(
					operation, tempVariableName);
			st.addAggr(
					"destinationVariable.{name, tmpName, nativeReturnType, size, returnObjectCreation}",
					variableName, tempVariableName, nativeReturnType, size,
					returnObjectCreation);
			st.addAggr("params.{name}", variableName);
		}
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		for (Variable variable : operation.getExternalVariables()) {
			if (!isSequential || variable.isFinal()) {
				st.addAggr("variables.{gVariableName, variableName}",
						this.getGlobalVariableName(variable.name, operation),
						variable.name);
			} else if (isSequential && !variable.isFinal()) {
				String allName = this.getAllocationName(variable.name,
						operation);
				st.addAggr("sequentialNonFinalVariables.{allName, arrName}",
						allName, variable.name);
			}
		}
		if (isSequential) {
			st.addAggr("allocation.{body}", this
					.createSequentialAllocationJavaFile(className, operation));
			st.addAggr("inputSize.{name, XYZ}",
					this.getInputXSizeVariableName(operation), "X");
			if (operation.variable.typeName.equals(BitmapImage.getInstance()
					.getClassName())
					|| operation.variable.typeName.equals(HDRImage
							.getInstance().getClassName())) {
				st.addAggr("inputSize.{name, XYZ}",
						this.getInputYSizeVariableName(operation), "Y");
			}
			st.addAggr("variables.{gVariableName, variableName}",
					this.getInputDataVariableName(operation),
					variableAllocation);
		}
		return st.render();
	}

	/**
	 * Create allocations for non-final variables in sequential operations.
	 */
	private String createSequentialAllocationJavaFile(String className,
			Operation operation) {
		ST st = new ST(templateSequentialAllocationJavaFile);
		st.add("kernelName", this.commonDefinitions.getKernelName(className));
		st.add("rsVarName", this.getRSVariableName());
		st.add("externalVariables", null);
		for (Variable variable : operation.getExternalVariables()) {
			if (!variable.isFinal()) {
				String allName = this.getAllocationName(variable.name,
						operation);
				String elementType = java2RSAllocationTypes
						.get(variable.typeName);
				String gName = this.getGlobalVariableName(variable.name,
						operation);
				String gNameOut = this.getOutputVariableName(variable.name,
						operation);
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
	 * Return a string containing the
	 */
	protected String getNativeReturnType(Operation operation) {
		if (operation.operationType == OperationType.Reduce) {
			return this.commonDefinitions
					.translateType(operation.variable.typeName);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
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
	abstract protected void fillReduceOperationCall(ST st, Operation operation);

	/**
	 * Fill the informed string template with all necessary data to create a
	 * valid foreach operation call.
	 */
	abstract protected void fillForeachOperationCall(ST st, Operation operation);

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
		String returnString = String.format("\treturn %s;",
				userFunctionVariable.name);
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
		if (!operation.getExternalVariables().isEmpty())
			externalVariables.append("\n");
		ret = externalVariables.toString()
				+ this.getOperationFunctionSignature(operation,
						FunctionType.BaseOperation)
				+ this.translateVariable(userFunctionVariable,
						this.cCodeTranslator.translate(code2Translate));
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelReduce(Operation operation) {
		ST st = new ST(templateParallelReduce);
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
		st.add("dataVar", this.getTileVariableName(operation));
		if (operation.variable.typeName.equals(BitmapImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(HDRImage.getInstance()
						.getClassName())) {
			st.add("sizeVar", this.getInputXSizeVariableName(operation));
		} else {
			// TODO Implement for Array type
			throw new RuntimeException("Not yet implemented for Array type.");
		}
		st.add("params", null);
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("params.{name}", variable.name);
		}
		st.add("destinationVar", this.commonDefinitions.getPrefix()
				+ operation.destinationVariable.name);
		return this.createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelReduceTile(Operation operation) {
		ST st = new ST(templateParallelReduceTile);
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
		st.add("dataVar", this.getInputDataVariableName(operation));
		if (operation.variable.typeName.equals(BitmapImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(HDRImage.getInstance()
						.getClassName())) {
			st.add("sizeVar", this.getInputYSizeVariableName(operation));
		} else {
			// TODO Implement for Array type
			throw new RuntimeException("Not yet implemented for Array type.");
		}
		st.add("params", null);
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("params.{name}", variable.name);
		}
		st.add("destinationVar", this.commonDefinitions.getPrefix()
				+ operation.destinationVariable.name);
		return this.createKernelFunction(operation, st.render(),
				FunctionType.Tile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateParallelReduceUserFunction(Operation operation) {
		String userCode = this.commonDefinitions.removeCurlyBraces(operation
				.getUserFunctionData().Code.trim());
		for (Variable userFunctionVariable : operation.getUserFunctionData().arguments) {
			userCode = this.translateVariable(userFunctionVariable,
					this.cCodeTranslator.translate(userCode));
		}
		return this.createSequentialAllocationRSFile(operation)
				+ this.createKernelFunction(operation, userCode,
						FunctionType.UserCode);
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
		ST st = new ST(templateSequentialForeach);
		st.add("functionSignature", this.getOperationFunctionSignature(
				operation, FunctionType.BaseOperation));
		String userFunctionVarType = this.commonDefinitions
				.translateType(userFunctionVariable.typeName);
		st.add("userFunctionVarName", userFunctionVariable.name);
		st.add("userFunctionVarType", userFunctionVarType);
		String cCode = this.translateVariable(userFunctionVariable,
				this.cCodeTranslator.translate(code2Translate)).trim();
		st.add("allocation", this.createSequentialAllocationRSFile(operation));
		st.add("externalVariables", null);
		// Must replace all user variables names in code by those new global
		// variables names
		for (Variable variable : operation.getExternalVariables()) {
			String gNameVar = this.getGlobalVariableName(variable.name,
					operation);
			if (!variable.isFinal()) {
				String gNameOut = this.getOutputVariableName(variable.name,
						operation);
				st.addAggr(
						"externalVariables.{ variableType, outVariableName, variableName }",
						variable.typeName, gNameOut, gNameVar);
			}
			cCode = cCode.replaceAll(variable.name, gNameVar);
		}
		String prefix = this.commonDefinitions.getPrefix();
		ST stFor = new ST(templateForLoop);
		stFor.add("varName", prefix + "x");
		stFor.add("varMaxVal", this.getInputXSizeVariableName(operation));
		ST stForBody = new ST(templateSequentialForLoopBody);
		stForBody.add("inputData", this.getInputDataVariableName(operation));
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
			stFor2.add("varMaxVal", this.getInputYSizeVariableName(operation));
			stFor2.add("body", stForBody.render());
			stFor.add("body", stFor2.render());
		} else {
			// Array types
			stFor.add("body", stForBody.render());
		}
		st.add("forLoop", stFor.render());
		return st.render();
	}

	private String createSequentialAllocationRSFile(Operation operation) {
		ST st = new ST(templateSequentialAllocationRSFile);
		st.add("inputData", this.getInputDataVariableName(operation));
		String inputXSize = this.getInputXSizeVariableName(operation);
		st.add("inputXSize", inputXSize);
		String inputYSize = this.getInputYSizeVariableName(operation);
		st.add("inputYSize", inputYSize);
		st.add("outVariable", null);
		st.add("externalVariables", null);
		if (operation.operationType == OperationType.Reduce) {
			st.addAggr("outVariable.{name}",
					this.getTileVariableName(operation));
		}
		for (Variable variable : operation.getExternalVariables()) {
			String gNameOut = this.getOutputVariableName(variable.name,
					operation);
			if (!variable.isFinal())
				st.addAggr("outVariable.{name}", gNameOut);
			String gNameVar = this.getGlobalVariableName(variable.name,
					operation);
			st.addAggr(
					"externalVariables.{ variableType, outVariableName, variableName }",
					variable.typeName, gNameOut, gNameVar);
		}
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
	 * Create a function signature for a given operation.
	 */
	@Override
	protected String getOperationFunctionSignature(Operation operation,
			FunctionType functionType) {
		Variable inputVar1 = operation.getUserFunctionData().arguments.get(0);
		String returnType = this.commonDefinitions
				.translateType(inputVar1.typeName);
		ST st;
		if (operation.operationType == OperationType.Foreach) {
			st = new ST(templateFunctionDecl);
			st.add("modifier", null);
			st.add("functionName",
					this.commonDefinitions.getOperationName(operation));
			if (operation.getExecutionType() == ExecutionType.Parallel) {
				st.add("returnType", returnType);
				st.add("isKernel", "");
				st.addAggr("params.{type, name}", returnType, inputVar1.name);
				st.addAggr("params.{type, name}", "uint32_t", "x");
				st.addAggr("params.{type, name}", "uint32_t", "y");
			} else {
				st.add("returnType", "void");
				st.add("isKernel", null);
				st.add("params", null);
			}
		} else if (operation.operationType == OperationType.Reduce) {
			st = this
					.initializeReduceSignatureTemplate(operation, functionType);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		return st.render();
	}

	private ST initializeReduceSignatureTemplate(Operation operation,
			FunctionType functionType) {
		ST st = new ST(templateFunctionDecl);
		String returnType = this.commonDefinitions.translateType(operation
				.getUserFunctionData().arguments.get(0).typeName);
		st.add("returnType", returnType);
		st.add("modifier", null);
		st.add("isKernel", null);
		st.add("params", null);
		if (functionType == FunctionType.BaseOperation) {
			if (operation.getExecutionType() == ExecutionType.Parallel) {
				st.add("isKernel", "");
				st.addAggr("params.{type, name}", "uint32_t", "x");
				st.add("functionName",
						this.commonDefinitions.getOperationName(operation));
			} else {
				st.add("returnType", "void");
			}
		} else if (functionType == FunctionType.Tile) {
			st.add("isKernel", "");
			st.addAggr("params.{type, name}", "uint32_t", "x");
			st.add("functionName", this.commonDefinitions
					.getOperationTileFunctionName(operation));
		} else {
			st.addAggr("modifier.{value}", "static");
			for (Variable inputVar : operation.getUserFunctionData().arguments)
				st.addAggr(
						"params.{type, name}",
						this.commonDefinitions.translateType(inputVar.typeName),
						inputVar.name);
			st.add("functionName", this.commonDefinitions
					.getOperationUserFunctionName(operation));
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
		String operationName = this.commonDefinitions
				.getOperationName(operation);
		String globalVariableName = this.upperCaseFirstLetter(variableName);
		return this.commonDefinitions.getPrefix() + "g" + globalVariableName
				+ this.upperCaseFirstLetter(operationName);
	}

	/**
	 * Change the first letter of the informed string to upper case.
	 */
	protected String upperCaseFirstLetter(String string) {
		return string.substring(0, 1).toUpperCase()
				+ string.substring(1, string.length());
	}

	protected String getTileVariableName(Operation operation) {
		return this.getGlobalVariableName("Tile", operation);
	}

	protected String getInputXSizeVariableName(Operation operation) {
		return this.getGlobalVariableName("InputXSize", operation);
	}

	protected String getInputYSizeVariableName(Operation operation) {
		return this.getGlobalVariableName("InputYSize", operation);
	}

	protected String getInputDataVariableName(Operation operation) {
		return this.getGlobalVariableName("Input", operation);
	}

	protected String getOutputDataVariableName(Operation operation) {
		return this.getGlobalVariableName("Output", operation);
	}

	protected String getRSVariableName() {
		return this.commonDefinitions.getPrefix() + "mRS";
	}

	protected String getOutputVariableName(String variableName,
			Operation operation) {
		return this.getGlobalVariableName(
				"output" + this.upperCaseFirstLetter(variableName), operation);
	}

	protected String getAllocationName(String variableName, Operation operation) {
		return this.getGlobalVariableName(variableName, operation)
				+ "_Allocation";
	}
}
