/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Parameter;
import org.parallelme.compiler.intermediate.UserFunction;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.userlibrary.classes.Array;

/**
 * Base class for array tests.
 * 
 * @author Wilson de Carvalho
 */
public abstract class ArrayTranslatorTest extends BaseTranslatorTest {
	private Variable arrayVar = new Variable("arrayVar", "Array",
			Arrays.asList(getParameterType()), "", 1);

	@Override
	protected Variable getUserLibraryVar() {
		return this.arrayVar;
	}

	@Override
	protected InputBind createInputBind() {
		List<Parameter> parameters = new ArrayList<>();
		parameters.add(new Variable("dataVar", getTranslatedParameterType()
				+ "[]", null, "", 2));
		parameters.add(new Variable(getParameterType() + ".class", "Class",
				Arrays.asList(getParameterType()), "", 3));
		return new InputBind(this.getUserLibraryVar(), 1, parameters, null,
				null);
	}

	@Override
	protected OutputBind createOutputBind(OutputBindType outputBindType) {
		Variable destinationVar = new Variable("arrayVar",
				getTranslatedParameterType() + "[]", null, "", 1);
		return new OutputBind(this.getUserLibraryVar(), destinationVar, 1,
				null, outputBindType);
	}

	@Override
	protected Operation createForeachOperation(ExecutionType executionType) {
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Foreach, null);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", getParameterType(), null, "", 10));
		UserFunction userFunction = new UserFunction(
				" { param1.value = 123; }", arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}

	@Override
	protected Operation createReduceOperation(ExecutionType executionType) {
		Variable destVar = new Variable("destVar", getParameterType(), null,
				"", 999);
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Reduce, destVar);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", getParameterType(), null, "", 10));
		arguments.add(new Variable("param2", getParameterType(), null, "", 11));
		UserFunction userFunction = new UserFunction(
				" { param2.value += param1.value; " + "return param2;}",
				arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}
	
	protected Operation createFilterOperation(ExecutionType executionType) {
		Variable destVar = new Variable("destVar", Array.getInstance()
				.getClassName(), Arrays.asList(getParameterType()), "", 999);
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Filter, destVar);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", getParameterType(), null, "", 10));
		UserFunction userFunction = new UserFunction(
				"{\n\treturn param1 > 2;\n}", arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}
}
