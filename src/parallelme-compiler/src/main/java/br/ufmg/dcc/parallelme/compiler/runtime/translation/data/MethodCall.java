/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime.translation.data;

import br.ufmg.dcc.parallelme.compiler.symboltable.TokenAddress;

/**
 * Represents a method call for a regular (non-iterator, non-bind) operation on
 * a given object.
 * 
 * @author Wilson de Carvalho
 */
public class MethodCall {
	public final String methodName;
	public final Variable variable;
	public final TokenAddress expressionAddress;

	public MethodCall(String methodName, Variable variable,
			TokenAddress statementAddress) {
		this.methodName = methodName;
		this.variable = variable;
		this.expressionAddress = statementAddress;
	}
}