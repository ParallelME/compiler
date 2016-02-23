/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime.translationdata;


/**
 * Stores user function data that must be translated to the target runtime.
 * 
 * @author Wilson de Carvalho
 */
public class UserFunction {
	public final String CCode;
	public final Variable variableArgument;

	public UserFunction(String CCode, Variable variableArgument) {
		this.CCode = CCode;
		this.variableArgument = variableArgument;
	}
}
