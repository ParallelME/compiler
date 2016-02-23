/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import br.ufmg.dcc.parallelme.compiler.runtime.translationdata.Iterator;
import br.ufmg.dcc.parallelme.compiler.runtime.translationdata.Variable;

/**
 * Code useful for specfic runtime definition implementation.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RuntimeDefinitionImpl implements RuntimeDefinition {
	private final String inSuffix = "In";
	private final String outSuffix = "Out";
	private final String functionName = "function";
	private final String prefix = "$";

	protected String getVariableInName(Variable variable) {
		return prefix + variable.name + inSuffix;
	}

	protected String getVariableOutName(Variable variable) {
		return prefix + variable.name + outSuffix;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFunctionName(int functionNumber) {
		return functionName + functionNumber;
	}

	protected String getPrefix() {
		return prefix;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateIteratorCode(Iterator iterator) {
		return this.getIteratorFunctionSignature(iterator)
				+ this.translateUserLibraryType(
						iterator.getUserFunctionData().variableArgument,
						iterator.getUserFunctionData().CCode);
	}

	/**
	 * Create the function signature for a given iterator.
	 * 
	 * @param iterator
	 *            Iterator that must be analyzed in order to create a function
	 *            signature.
	 * 
	 * @return Function signature.
	 */
	abstract protected String getIteratorFunctionSignature(Iterator iterator);
}
