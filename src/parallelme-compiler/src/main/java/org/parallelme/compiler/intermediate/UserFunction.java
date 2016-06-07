/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

import java.util.List;

/**
 * Stores user function data that must be translated to the target runtime.
 * 
 * @author Wilson de Carvalho
 */
public class UserFunction {
	public final String Code;
	public final List<Variable> arguments;

	public UserFunction(String Code, List<Variable> arguments) {
		this.Code = Code;
		this.arguments = arguments;
	}
}
