/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime.translation;

/**
 * 
 * 
 * @author Wilson de Carvalho
 */
public class SimpleTranslator implements CTranslator {

	/**
	 * {@inheritDoc}
	 * 
	 * It is important to note that this translator will simply perform function
	 * replacement and won't perform syntactic analysis
	 */
	@Override
	public String translate(String javaCode) {
		String ret = this.replaceMathFunctions(javaCode);
		return ret;
	}

	private String replaceMathFunctions(String javaCode) {
		return javaCode.replaceAll("Math.", "");
	}
}
