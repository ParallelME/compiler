/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

/**
 * Intermediate representation for input bind operations.
 * 
 * @author Wilson de Carvalho
 */
public class InputBind extends UserLibraryData {
	private Parameter[] parameters;

	public InputBind(Variable variable, int sequentialNumber,
			Parameter[] parameters) {
		super(variable, sequentialNumber);
		this.setParameters(parameters);
	}

	public Parameter[] getParameters() {
		return parameters;
	}

	public void setParameters(Parameter[] parameters) {
		this.parameters = parameters;
	}

	@Override
	public int hashCode() {
		return this.parameters.hashCode();
	}
}
