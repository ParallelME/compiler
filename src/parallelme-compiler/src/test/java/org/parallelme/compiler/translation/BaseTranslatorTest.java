/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.userlibrary.BaseUserLibraryTranslator;

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
	 * Returns a user library variable for this test class.
	 */
	abstract protected Variable getUserLibraryVar();

	abstract protected BaseUserLibraryTranslator getTranslator();

	protected Variable createExternalVariable(String modifier) {
		return new Variable("externalVar", "int", "", modifier, 10);
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
}
