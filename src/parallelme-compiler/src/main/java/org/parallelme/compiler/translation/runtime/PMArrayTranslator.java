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

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.ArrayTranslator;
import org.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import org.stringtemplate.v4.ST;

/**
 * Definitions for Array translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public class PMArrayTranslator extends PMTranslator implements ArrayTranslator {
	private static final String templateInputBindObjCreation = "<arrayPointer> = ParallelMERuntime.getInstance().createArray(<arrayName>);";
	private static final String templateOutputBindCall1 = "<baseType>[] <name> = new <baseType>[ParallelMERuntime.getInstance().getLength(<arrayPointer>)];\n"
			+ "ParallelMERuntime.getInstance().toArray(<arrayPointer>, <name>);\n"
			+ "return <name>;";
	private static final String templateOutputBindCall2 = "ParallelMERuntime.getInstance().toArray(<arrayPointer>, <arrayName>);";
	private static final String templateOperationCall = "<destinationVariable:{var|<var.nativeReturnType>[] <var.name> = new <var.nativeReturnType>[<var.size>];\n}>"
			+ "<operationName>(<params:{var|<var.name>}; separator=\", \">);"
			+ "<destinationVariable:{var|\n\nreturn new <var.methodReturnType>(<var.name>[0]);}>";

	public PMArrayTranslator(CTranslator cCodeTranslator) {
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
		ST st = new ST(templateInputBindObjCreation);
		st.add("arrayPointer",
				this.commonDefinitions.getPointerName(inputBind.variable));
		Variable variable = (Variable) inputBind.parameters.get(0);
		st.add("arrayName", variable.name);
		return st.render();
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
		String arrayPointer = this.commonDefinitions
				.getPointerName(outputBind.variable);
		// If it is an object assignment, must declare the destination
		// object type and name.
		ST st;
		if (outputBind.outputBindType != OutputBindType.None) {
			st = new ST(templateOutputBindCall1);
			st.add("name", commonDefinitions.getPrefix() + "javaArray");
			String baseType = outputBind.destinationObject.typeName
					.replaceAll("\\[", "").replaceAll("\\]", "").trim();
			st.add("baseType", baseType);
			st.add("type", outputBind.destinationObject.typeName);
			st.add("arrayPointer", arrayPointer);
		} else {
			st = new ST(templateOutputBindCall2);
			st.add("arrayPointer", arrayPointer);
			st.add("arrayName", outputBind.destinationObject.name);
		}
		return st.render();
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
	public String translateOperationCall(String className, Operation operation) {
		ST st = new ST(templateOperationCall);
		this.fillOperationCallBaseInfo(st, operation);
		if (operation.destinationVariable != null) {
			String nativeReturnType = "";
			String methodReturnType = "";
			String size = "";
			String variableName = this.commonDefinitions.getPrefix()
					+ operation.destinationVariable.name;
			// These variables must be set to different values when map and
			// filter operations are implemented.
			if (operation.operationType == OperationType.Reduce) {
				nativeReturnType = this.commonDefinitions
						.translateToCType(operation.variable.typeParameters
								.get(0));
				methodReturnType = UserLibraryClassFactory.getClass(
						operation.variable.typeParameters.get(0))
						.getClassName();
				size = "1";
			}
			st.addAggr(
					"destinationVariable.{name, nativeReturnType, methodReturnType, size}",
					variableName, nativeReturnType, methodReturnType, size);
			st.addAggr("params.{name}", variableName);
		} else {
			st.add("destinationVariable", null);
		}
		return st.render();
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
		// Takes the first var, since they must be the same for reduce
		// operations
		String varType = this.commonDefinitions
				.translateToCType(inputVar1.typeName);
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		String dataVar = isSequential ? getDataVariableName()
				: getTileVariableName();
		st.add("destinationVar", this.commonDefinitions.getPrefix()
				+ operation.destinationVariable.name);
		stForBody.add("dataVar", dataVar);
		st.addAggr("decl.{expression}",
				getExpression(varType, inputVar1.name, dataVar + "[0]"));
		st.addAggr("decl.{expression}",
				getExpression(varType, inputVar2.name, ""));
		setExternalVariables(stForBody, operation, false);
		stForLoop.add("initValue", "1");
		stForLoop.add("varMaxVal", isSequential ? getLengthVariableName()
				: getTileSizeVariableName());
		stForLoop.add("body", stForBody.render());
		st.addAggr("forLoop.{loop}", stForLoop.render());
		stForBody.remove("dataVar");
		stForBody.add("dataVar", getDataVariableName());
		if (!isSequential) {
			ST stForLoop2 = new ST(templateForLoop);
			stForLoop2.add("initValue", String.format(
					"(int) pow(floor(sqrt((float)%s)), 2)",
					getLengthVariableName()));
			stForLoop2.add("varName", xVar);
			stForLoop2.add("varMaxVal", getLengthVariableName());
			stForLoop2.add("body", stForBody.render());
			st.addAggr("forLoop.{loop}", stForLoop2.render());
		}
		return createKernelFunction(operation, st.render(),
				FunctionType.BaseOperation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaInterfaceImports() {
		ArrayList<String> ret = new ArrayList<>();
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaClassImports() {
		return this.getJavaInterfaceImports();
	}
}
