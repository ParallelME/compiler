/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation;

import java.util.List;

/**
 * Base class for user library translators.
 * 
 * @author Wilson de Carvalho
 */
abstract public class BaseTranslatorTest {
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
}
