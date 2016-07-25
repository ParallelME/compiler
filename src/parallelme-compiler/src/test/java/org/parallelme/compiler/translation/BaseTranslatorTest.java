/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.UserFunction;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;
import org.parallelme.compiler.userlibrary.classes.Array;

/**
 * Base class for user library translators.
 * 
 * @author Wilson de Carvalho
 */
abstract public class BaseTranslatorTest {
	protected String className = "SomeClass";
	protected RuntimeCommonDefinitions commonDefinitions = RuntimeCommonDefinitions
			.getInstance();

	/**
	 * A user-library type parameter that will be used to create user library
	 * Arrays.
	 */
	abstract protected String getParameterType();

	/**
	 * A map type is a type different than that provided by getParameterType()
	 * that will be used to create map functions.
	 */
	abstract protected String getMapType();

	/**
	 * The Java equivalent type provided in getParameterType().
	 */
	abstract protected String getTranslatedParameterType();

	/**
	 * The Java equivalent type provided in getMapType().
	 */
	abstract protected String getTranslatedMapType();

	abstract protected InputBind createInputBind();

	abstract protected OutputBind createOutputBind(OutputBindType outputBindType);

	/**
	 * Returns a user library variable for this test class.
	 */
	abstract protected Variable getUserLibraryVar();

	abstract protected BaseUserLibraryTranslator getTranslator();

	abstract protected Operation createForeachOperation(
			ExecutionType executionType);

	abstract protected Operation createReduceOperation(
			ExecutionType executionType);

	protected Operation createMapOperation(ExecutionType executionType) {
		Variable destVar = new Variable("destVar", Array.getInstance()
				.getClassName(), Arrays.asList(getMapType()), "", 999);
		Operation operation = new Operation(this.getUserLibraryVar(), 123,
				null, OperationType.Map, destVar);
		operation.setExecutionType(executionType);
		List<Variable> arguments = new ArrayList<>();
		arguments.add(new Variable("param1", getParameterType(), null, "", 10));
		UserFunction userFunction = new UserFunction(String.format(
				"{\n\t%s ret;\n" + "\tret = param1 * 1.5f;\n\treturn ret;\n}",
				getTranslatedMapType()), arguments);
		operation.setUserFunctionData(userFunction);
		return operation;
	}

	protected Variable createExternalVariable(String modifier, String name) {
		return new Variable(name, "int", null, modifier, 10);
	}

	protected MethodCall createMethodCall(String methodName) {
		return new MethodCall(methodName, this.getUserLibraryVar(), null, 999);
	}

	/**
	 * Flattens the input string, removing new line marks, spaces and tabs.
	 */
	protected String flattenString(String inputString) {
		return inputString.replaceAll(" ", "").replaceAll("\t", "")
				.replaceAll("\n", "").replaceAll("\r", "");
	}

	/**
	 * Flattens the input list of strings transforming it into a single string,
	 * removing new line marks, spaces and tabs.
	 */
	protected String flattenString(List<String> inputString) {
		StringBuilder tmp = new StringBuilder();
		for (String str : inputString)
			tmp.append(str);
		return tmp.toString().replaceAll(" ", "").replaceAll("\t", "")
				.replaceAll("\n", "").replaceAll("\r", "");
	}

	/**
	 * Checks if the expected translation is equivalent to the translated code.
	 */
	protected void validateTranslation(String expectedTranslation,
			String translatedCode) {
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedCode));
	}

	/**
	 * Checks if the expected translation is equivalent to the translated code.
	 */
	protected void validateTranslation(String expectedTranslation,
			List<String> translatedCode) {
		assertEquals(this.flattenString(expectedTranslation),
				this.flattenString(translatedCode));
	}

	/**
	 * 
	 */
	protected String upperCaseFirstLetter(String string) {
		return string.substring(0, 1).toUpperCase()
				+ string.substring(1, string.length());
	}
}
