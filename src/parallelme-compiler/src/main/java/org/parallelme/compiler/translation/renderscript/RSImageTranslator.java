/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.ImageTranslator;
import org.parallelme.compiler.userlibrary.classes.Pixel;
import org.stringtemplate.v4.ST;

/**
 * Base class for Image translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSImageTranslator extends RSTranslator implements
		ImageTranslator {
	private static final String templateOutputBindCall1 = "Bitmap <destinationObject> = Bitmap.createBitmap(<inputAllocation>.getType().getX(), "
			+ "<inputAllocation>.getType().getY(), Bitmap.Config.ARGB_8888);\n"
			+ "return <destinationObject>;";
	private static final String templateOutputBindCall2 = "<kernelName>.forEach_toBitmap<classType>(<outputObject>, <inputObject>);\n"
			+ "<inputObject>.copyTo(<destinationObject>);";

	public RSImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateObjDeclaration(InputBind inputBind) {
		String inAllocation = this.commonDefinitions
				.getVariableInName(inputBind.variable);
		String outAllocation = this.commonDefinitions
				.getVariableOutName(inputBind.variable);
		return String.format("private Allocation %s, %s;", inAllocation,
				outAllocation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateObjDeclaration(Operation operation) {
		if (operation.operationType == OperationType.Map
				|| operation.operationType == OperationType.Filter) {
			String outAllocation = this.commonDefinitions
					.getVariableOutName(operation.destinationVariable);
			return String.format("private Allocation %s;", outAllocation);
		} else {
			return "";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBindCall(String className,
			OutputBind outputBind) {
		StringBuilder ret = new StringBuilder();
		String inputObject = this.commonDefinitions
				.getVariableInName(outputBind.variable);
		String outputObject = this.commonDefinitions
				.getVariableOutName(outputBind.variable);
		String destinationObject = outputBind.destinationObject.name;
		// If it is an object assignment, must declare the destination
		// object type and name.
		if (outputBind.outputBindType != OutputBindType.None) {
			ST st = new ST(templateOutputBindCall1);
			st.add("inputAllocation", inputObject);
			st.add("destinationObject", destinationObject);
			ret.append(st.render());
		} else {
			ST st = new ST(templateOutputBindCall2);
			st.add("classType", outputBind.variable.typeName);
			st.add("kernelName",
					this.commonDefinitions.getKernelName(className));
			st.add("outputObject", outputObject);
			st.add("inputObject", inputObject);
			st.add("destinationObject", destinationObject);
			ret.append(st.render());
		}
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getDestinationArraySize(Operation operation) {
		if (operation.operationType == OperationType.Reduce) {
			return "4";
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getReturnObjectCreation(Operation operation,
			String variableName) {
		if (operation.operationType == OperationType.Reduce) {
			return String.format("%s(%s[0], %s[1], %s[2], %s[3], -1, -1)",
					Pixel.getInstance().getClassName(), variableName,
					variableName, variableName, variableName);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fillReduceOperationCall(ST st, Operation operation) {
		super.fillReduceOperationCall(st, operation);
		st.addAggr("inputSize.{name, XYZ, allocationName}",
				this.getInputYSizeVariableName(operation), "Y",
				commonDefinitions.getVariableOutName(operation.variable));
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			st.addAggr("tileSize.{name, expression}",
					getTileSizeVariableName(operation), String.format(
							"%s.getType().getY()", commonDefinitions
									.getVariableOutName(operation.variable)));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateReduce(Operation operation) {
		ST st = new ST(templateReduce);
		ST stForLoop = new ST(templateForLoop);
		ST stForBody = new ST(templateReduceForBody);
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
		String varType = commonDefinitions.translateType(inputVar1.typeName);
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
		stForLoop.add("varMaxVal",
				getAllocationDimCall("X", getInputDataVariableName(operation)));
		if (isSequential) {
			String yVar = commonDefinitions.getPrefix() + "y";
			stForBody.addAggr("yVar.{name}", yVar);
			ST stInnerFor = new ST(templateForLoop);
			stInnerFor.add("varName", yVar);
			stInnerFor.add("initValue", "1");
			stInnerFor.add(
					"varMaxVal",
					getAllocationDimCall("Y",
							getInputDataVariableName(operation)));
			stInnerFor.add("body", stForBody.render());
			stForLoop.add("body", stInnerFor.render());
		} else {
			stForBody.add("yVar", null);
			stForLoop.add("body", stForBody.render());
		}
		st.addAggr("forLoop.{loop}", stForLoop.render());
		stForBody.remove("dataVar");
		stForBody.add("dataVar", getInputDataVariableName(operation));
		st.add("destVar",
				getOutputVariableName(operation.destinationVariable, operation));
		setExternalVariables(operation, st);
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}
}
