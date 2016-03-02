/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime.translation;

import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Variable;

/**
 * Contract for user library types translation.
 * 
 * @author Wilson de Carvalho
 */
public interface BaseType {
	/**
	 * Translates a given input.
	 * 
	 * @param input
	 * @return
	 */
	public String translate(String input);

	/**
	 * Replace a given variable in the code informed.
	 * 
	 * @param variable
	 *            Variable that must be replaced.
	 * @param code
	 *            Code on which the variable must be replaced.
	 * @return New code with variable replaced.
	 */
	public String replaceVariable(Variable variable, String code);
}
