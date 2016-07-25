/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.ArrayTranslator;
import org.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import org.stringtemplate.v4.ST;

/**
 * Definitions for Array translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public class RSArrayTranslator extends RSTranslator implements ArrayTranslator {
	private static final String templateInputBindObjCreation = "<allocation> = Allocation.createSized(PM_mRS, Element.<elementType>(PM_mRS), <inputArray>.length);\n"
			+ "<allocation>.copyFrom(<inputArray>);";
	private static final String templateOutputBindCall1 = "<baseType>[] <name>;\n"
			+ "if (<inputAllocation> != null) {\n"
			+ "\t<name> = new <baseType>[<inputAllocation>.getType().getX()];\n"
			+ "\t<inputAllocation>.copyTo(<name>);\n"
			+ "} else {\n"
			+ "\t<name> = new <baseType>[0];\n" + "}\n" + "return <name>;";
	private static final String templateOutputBindCall2 = "<inputObject>.copyTo(<destinationObject>);";

	public RSArrayTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBind(String className, InputBind inputBind) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjCreation(String className,
			InputBind inputBind) {
		String inputObject = commonDefinitions
				.getVariableOutName(inputBind.variable);
		ST st = new ST(templateInputBindObjCreation);
		// TODO Check if parameters array has size 1, otherwise throw an
		// exception and abort translation.
		st.add("inputArray", inputBind.parameters.get(0));
		st.add("allocation", inputObject);
		st.add("elementType",
				getRenderScriptJavaType(inputBind.variable.typeParameters
						.get(0)));
		return st.render();
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
				|| operation.operationType == OperationType.Filter) {
			return translateObjDeclaration(operation.destinationVariable);
		} else {
			return "";
		}
	}

	/**
	 * Returns the creation statement for a given variable's allocation.
	 */
	private String translateObjDeclaration(Variable variable) {
		String inAllocation = commonDefinitions.getVariableOutName(variable);
		return String.format("private Allocation %s;", inAllocation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBind(String className, OutputBind outputBind) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBindCall(String className,
			OutputBind outputBind) {
		StringBuilder ret = new StringBuilder();
		String allocationObject = commonDefinitions
				.getVariableOutName(outputBind.variable);
		String destinationObject = outputBind.destinationObject.name;
		// If it is an object assignment, must declare the destination
		// object type and name.
		if (outputBind.outputBindType != OutputBindType.None) {
			ST st = new ST(templateOutputBindCall1);
			st.add("name", commonDefinitions.getPrefix() + "javaArray");
			String baseType = outputBind.destinationObject.typeName
					.replaceAll("\\[", "").replaceAll("\\]", "").trim();
			st.add("baseType", baseType);
			st.add("inputAllocation", allocationObject);
			ret.append(st.render());
		} else {
			ST st = new ST(templateOutputBindCall2);
			st.add("inputObject", allocationObject);
			st.add("destinationObject", destinationObject);
			ret.append(st.render());
		}
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(String className, MethodCall methodCall) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateReduce(Operation operation) {
		ST st = new ST(templateReduce);
		ST stForLoop = new ST(templateForLoop);
		ST stForLoop2 = new ST(templateForLoop);
		ST stForBody = new ST(templateReduceForBody);
		stForBody.add("yVar", null);
		String xVar = commonDefinitions.getPrefix() + "x";
		stForLoop.add("varName", xVar);
		stForBody.add("xVar", xVar);
		Variable inputVar1 = operation.getUserFunctionData().arguments.get(0);
		Variable inputVar2 = operation.getUserFunctionData().arguments.get(1);
		stForBody.add("inputVar1", inputVar1.name);
		st.add("inputVar1", inputVar1.name);
		stForBody.add("inputVar2", inputVar2.name);
		st.add("inputVar2", inputVar2.name);
		stForBody.add("userFunctionName",
				commonDefinitions.getOperationUserFunctionName(operation));
		// Takes the first var, since they must be the same for reduce
		// operations
		String varType = commonDefinitions.translateToCType(inputVar1.typeName);
		st.add("varType", varType);
		stForBody.add("varType", varType);
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		String dataVar = isSequential ? getInputDataVariableName(operation)
				: getTileVariableName(operation);
		st.add("destinationVar", commonDefinitions.getPrefix()
				+ operation.destinationVariable.name);
		st.add("dataVar", dataVar);
		stForBody.add("dataVar", dataVar);
		stForBody.add("params", null);
		for (Variable variable : operation.getExternalVariables()) {
			stForBody.addAggr("params.{name}", variable.name);
			if (isSequential && !variable.isFinal()) {
				stForBody.addAggr("params.{name}",
						commonDefinitions.getPrefix() + variable.name);
			}
		}
		stForLoop.add("initValue", "1");
		String inputDataVariableName = getInputDataVariableName(operation);
		String varMaxVal = getAllocationDimCall("X",
				isSequential ? inputDataVariableName
						: getTileVariableName(operation));
		stForLoop.add("varMaxVal", varMaxVal);
		stForLoop.add("body", stForBody.render());
		st.addAggr("forLoop.{loop}", stForLoop.render());
		stForBody.remove("dataVar");
		stForBody.add("dataVar", getInputDataVariableName(operation));
		if (!isSequential) {
			stForLoop2.add("initValue", String.format(
					"(int) pow(floor(sqrt((float)%s)), 2)",
					getAllocationDimCall("X", inputDataVariableName)));
			stForLoop2.add("varName", xVar);
			stForLoop2.add("varMaxVal",
					getAllocationDimCall("X", inputDataVariableName));
			stForLoop2.add("body", stForBody.render());
			st.addAggr("forLoop.{loop}", stForLoop2.render());
		}
		setExternalVariables(operation, st);
		st.add("destVar",
				getOutputVariableName(operation.destinationVariable, operation));
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaInterfaceImports() {
		return new ArrayList<>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaClassImports() {
		return getJavaInterfaceImports();
	}

	@Override
	protected String getDestinationArraySize(Operation operation) {
		return "1";
	}

	@Override
	protected String getReturnObjectCreation(Operation operation,
			String variableName) {
		String ret;
		if (operation.operationType == OperationType.Reduce) {
			ret = String.format(
					"%s(%s[0])",
					UserLibraryClassFactory.getClass(
							operation.variable.typeParameters.get(0))
							.getClassName(), variableName);
		} else if (operation.operationType == OperationType.Map
				|| operation.operationType == OperationType.Filter) {
			ret = String.format(
					"%s(%s)",
					UserLibraryClassFactory.getClass(
							operation.variable.typeParameters.get(0))
							.getClassName(), variableName);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		return ret;
	}

	@Override
	protected void fillReduceOperationCall(ST st, Operation operation) {
		super.fillReduceOperationCall(st, operation);
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			st.addAggr("tileSize.{name, expression}",
					getTileSizeVariableName(operation), String.format(
							"(int)Math.floor(Math.sqrt(%s.getType().getX()))",
							commonDefinitions
									.getVariableOutName(operation.variable)));
		}
	}
}
