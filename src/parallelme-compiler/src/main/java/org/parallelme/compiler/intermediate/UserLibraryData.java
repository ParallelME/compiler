/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

/**
 * Defines the basic user library data that must be passed to the runtime to
 * translate the code.
 * 
 * @author Wilson de Carvalho
 */
public class UserLibraryData {
	private Variable variable;
	// A sequential number that is used to uniquely identify this iterator.
	public final int sequentialNumber;

	public UserLibraryData(Variable variable, int sequentialNumber) {
		this.setVariable(variable);
		this.sequentialNumber = sequentialNumber;
	}

	public Variable getVariable() {
		return variable;
	}

	public void setVariable(Variable variable) {
		this.variable = variable;
	}

	@Override
	public int hashCode() {
		return this.variable.hashCode() * 3 + this.sequentialNumber * 7;
	}
}
