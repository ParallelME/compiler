/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.UserFunction;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;

/**
 * Base class for image tests.
 * 
 * @author Wilson de Carvalho
 */
public abstract class ImageTranslatorTest extends BaseTranslatorTest {
	protected Operation createForeachOperation(ExecutionType executionType) {
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Foreach, null);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", "Pixel", null, "", 10));
		UserFunction userFunction = new UserFunction(
				" { param1.rgba.red = 123; }", arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}

	protected Operation createReduceOperation(ExecutionType executionType) {
		Variable destVar = new Variable("destVar", "Pixel", null, "", 999);
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Reduce, destVar);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", "Pixel", null, "", 10));
		arguments.add(new Variable("param2", "Pixel", null, "", 11));
		UserFunction userFunction = new UserFunction(
				" { param1.rgba.red = 123; param2.rgba.green = 456; "
						+ "return param2;}", arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}
}
