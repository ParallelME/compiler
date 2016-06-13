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
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Variable;
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
	private static final String templateOutputBindCall1 = "<destinationObject> = Bitmap.createBitmap(<inputAllocation>.getType().getX(), "
			+ "<inputAllocation>.getType().getY(), Bitmap.Config.ARGB_8888);\n";
	private static final String templateOutputBindCall2 = "<kernelName>.forEach_toBitmap<classType>(<outputObject>, <inputObject>);\n"
			+ "<inputObject>.copyTo(<destinationObject>);";
	private static final String templateAllocation = "Type <typeName>Type = new Type.Builder(<rsVarName>, Element.<rsType>(<rsVarName>))\n"
			+ "\t.set<XYZ>(<expression>)\n"
			+ "\t.create();\n"
			+ "Allocation <varAllocationName> = Allocation.createTyped(<rsVarName>, <typeName>Type);";

	public RSImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjDeclaration(InputBind inputBind) {
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
		}
		ST st = new ST(templateOutputBindCall2);
		st.add("classType", outputBind.variable.typeName);
		st.add("kernelName", this.commonDefinitions.getKernelName(className));
		st.add("outputObject", outputObject);
		st.add("inputObject", inputObject);
		st.add("destinationObject", destinationObject);
		ret.append(st.render());

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
		st.addAggr("allocation.{body}", this.createTileAllocation(operation));
		st.addAggr("allocation.{body}", this.createReturnAllocation(operation));
		String tileVariableName = this.getTileVariableName(operation);
		st.addAggr("kernels.{functionName, allocations}",
				this.commonDefinitions.getOperationTileFunctionName(operation),
				tileVariableName);
		st.addAggr("kernels.{functionName, allocations}",
				this.commonDefinitions.getOperationName(operation),
				this.commonDefinitions.getPrefix()
						+ operation.destinationVariable.name);
		st.addAggr("variables.{gVariableName, variableName}",
				this.getInputDataVariableName(operation),
				this.commonDefinitions.getVariableOutName(operation.variable));
		st.addAggr("variables.{gVariableName, variableName}", tileVariableName,
				tileVariableName);
		st.addAggr("inputSize.{name, XYZ}",
				this.getInputXSizeVariableName(operation), "X");
		st.addAggr("inputSize.{name, XYZ}",
				this.getInputYSizeVariableName(operation), "Y");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fillForeachOperationCall(ST st, Operation operation) {
		String outputAllocation = this.commonDefinitions
				.getVariableOutName(operation.variable);
		st.addAggr("kernels.{functionName, allocations}",
				this.commonDefinitions.getOperationName(operation),
				outputAllocation + ", " + outputAllocation);
	}

	private String createTileAllocation(Operation operation) {
		ST st = new ST(templateAllocation);
		// For typed classes like Array, get the type parameter
		String javaType = operation.variable.typeParameterName.isEmpty() ? operation.variable.typeName
				: operation.variable.typeParameterName;
		String tileVariableName = this.getTileVariableName(operation);
		st.add("typeName", tileVariableName);
		st.add("rsVarName", this.getRSVariableName());
		st.add("rsType", java2RSAllocationTypes.get(javaType));
		st.add("XYZ", "X");
		st.add("expression",
				this.commonDefinitions.getVariableOutName(operation.variable)
						+ ".getType().getX()");
		st.add("varAllocationName", tileVariableName);
		return st.render();
	}

	private String createReturnAllocation(Operation operation) {
		ST st = new ST(templateAllocation);
		// For typed classes like Array, get the type parameter
		String javaType = operation.variable.typeParameterName.isEmpty() ? operation.variable.typeName
				: operation.variable.typeParameterName;
		Variable destvar = operation.destinationVariable;
		st.add("typeName", this.commonDefinitions.getPrefix() + destvar.name);
		st.add("rsVarName", this.getRSVariableName());
		st.add("rsType", java2RSAllocationTypes.get(javaType));
		st.add("XYZ", "X");
		st.add("expression", "1");
		st.add("varAllocationName", this.commonDefinitions.getPrefix()
				+ operation.destinationVariable.name);
		return st.render();
	}
}
