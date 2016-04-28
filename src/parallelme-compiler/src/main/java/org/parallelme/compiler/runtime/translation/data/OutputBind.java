/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime.translation.data;

import org.parallelme.compiler.symboltable.TokenAddress;

/**
 * Defines the output bind data that must be passed to the runtime to translate
 * the code.
 * 
 * @author Wilson de Carvalho
 */
public class OutputBind extends UserLibraryData {
	public final Variable destinationObject;
	public final TokenAddress statementAddress;
	// Indicates if the output bind statement is also an object declaration
	public final boolean isObjectDeclaration;

	public OutputBind(Variable variable, Variable destinationObject,
			int sequentialNumber, TokenAddress statementAddress,
			boolean isObjectDeclaration) {
		super(variable, sequentialNumber);
		this.statementAddress = statementAddress;
		this.destinationObject = destinationObject;
		this.isObjectDeclaration = isObjectDeclaration;
	}

	@Override
	public int hashCode() {
		return this.destinationObject.hashCode() * 3
				+ this.statementAddress.hashCode() * 7;
	}
}
