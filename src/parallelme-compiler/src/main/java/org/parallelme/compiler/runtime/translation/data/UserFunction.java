/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime.translation.data;

/**
 * Stores user function data that must be translated to the target runtime.
 * 
 * @author Wilson de Carvalho
 */
public class UserFunction {
	public final String Code;
	public final Variable variableArgument;

	public UserFunction(String Code, Variable variableArgument) {
		this.Code = Code;
		this.variableArgument = variableArgument;
	}

	@Override
	public int hashCode() {
		return this.Code.hashCode() * 3 + this.variableArgument.hashCode() * 7;
	}
}
