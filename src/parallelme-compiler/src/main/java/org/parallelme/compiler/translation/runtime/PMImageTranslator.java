/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.HDRImageTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Definitions for Image translation to ParallelME runtime.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMImageTranslator extends PMTranslator implements
		HDRImageTranslator {
	private static final String templateMethodCall = "return ParallelMERuntime.getInstance().<methodName>(<params:{var|<var.name>}; separator=\", \">);";
	private static final String templateOutputBindCall1 = "<bitmapVar> = Bitmap.createBitmap(\n"
			+ "\tParallelMERuntime.getInstance().getWidth(<imagePointer>),\n"
			+ "\tParallelMERuntime.getInstance().getHeight(<imagePointer>),\n"
			+ "\tBitmap.Config.ARGB_8888);\n";
	private static final String templateOutputBindCall2 = "ParallelMERuntime.getInstance().toBitmap<className>(<imagePointer>, <bitmapName>);";

	public PMImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBindCall(String className,
			OutputBind outputBind) {
		StringBuilder ret = new StringBuilder();
		// If it is an object assignment, must declare the destination
		// object type and name.
		if (outputBind.outputBindType != OutputBindType.None) {
			ST st = new ST(templateOutputBindCall1);
			st.add("bitmapVar", outputBind.destinationObject.name);
			st.add("imagePointer",
					this.commonDefinitions.getPointerName(outputBind.variable));
			ret.append(st.render());
		}
		ST st = new ST(templateOutputBindCall2);
		st.add("imagePointer",
				this.commonDefinitions.getPointerName(outputBind.variable));
		st.add("className", outputBind.variable.typeName);
		st.add("bitmapName", outputBind.destinationObject.name);
		ret.append(st.render());
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(String className, MethodCall methodCall) {
		if (methodCall.methodName.equals(HDRImage.getInstance()
				.getHeightMethodName())
				|| methodCall.methodName.equals(HDRImage.getInstance()
						.getWidthMethodName())
				|| methodCall.methodName.equals(BitmapImage.getInstance()
						.getHeightMethodName())
				|| methodCall.methodName.equals(BitmapImage.getInstance()
						.getWidthMethodName())) {
			ST st = new ST(templateMethodCall);
			st.add("methodName", methodCall.methodName);
			st.addAggr("params.{name}",
					this.commonDefinitions.getPointerName(methodCall.variable));
			return st.render();
		}
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String translateReduce(Operation operation) {
		ST st = new ST(templateReduce);
		ST stForLoop = new ST(templateForLoop);
		ST stForBody = new ST(templateReduceForBody);
		String xVar = this.commonDefinitions.getPrefix() + "x";
		stForLoop.add("varName", xVar);
		stForBody.add("xVar", xVar);
		Variable inputVar1 = operation.getUserFunctionData().arguments.get(0);
		Variable inputVar2 = operation.getUserFunctionData().arguments.get(1);
		stForBody.add("inputVar1", inputVar1.name);
		st.add("inputVar1", inputVar1.name);
		stForBody.add("inputVar2", inputVar2.name);
		stForBody.add("userFunctionName",
				this.commonDefinitions.getOperationUserFunctionName(operation));
		st.add("destinationVar", commonDefinitions.getDataReturnVarName());
		// Takes the first var, since they must be the same for reduce
		// operations
		String varType = this.commonDefinitions
				.translateToCType(inputVar1.typeName);
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		String dataVar = isSequential ? commonDefinitions.getDataVarName()
				: getTileVariableName();
		stForBody.add("dataVar", dataVar);
		st.addAggr("decl.{expression}",
				getExpression(varType, inputVar1.name, dataVar + "[0]"));
		st.addAggr("decl.{expression}",
				getExpression(varType, inputVar2.name, ""));
		if (isSequential)
			stForLoop.add("varMaxVal", this.getWorkSizeVariableName());
		else
			stForLoop.add("varMaxVal", this.getHeightVariableName());
		if (isSequential) {
			st.addAggr(
					"decl.{expression}",
					getExpression("int", getWorkSizeVariableName(),
							getHeightVariableName() + "*"
									+ getWidthVariableName()));
		}
		setExternalVariables(stForBody, operation, false);
		if (isSequential) {
			stForLoop.add("initValue", "0");
		} else {
			stForLoop.add("initValue", "1");
		}
		stForLoop.add("body", stForBody.render());
		st.addAggr("forLoop.{loop}", stForLoop.render());
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}
}
