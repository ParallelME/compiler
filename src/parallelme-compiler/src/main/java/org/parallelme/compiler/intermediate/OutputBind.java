/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

import org.parallelme.compiler.symboltable.TokenAddress;

/**
 * Intermediate representation for output bind operations.
 * 
 * @author Wilson de Carvalho
 */
public class OutputBind extends UserLibraryData {
	public enum OutputBindType {
		Assignment, DeclarativeAssignment, None;
	}

	public final Variable destinationObject;
	public final TokenAddress statementAddress;
	// Indicates if the output bind statement is also an object assignment
	public final OutputBindType outputBindType;

	public OutputBind(Variable variable, Variable destinationObject,
			int sequentialNumber, TokenAddress statementAddress,
			OutputBindType outputBindType) {
		super(variable, sequentialNumber);
		this.statementAddress = statementAddress;
		this.destinationObject = destinationObject;
		this.outputBindType = outputBindType;
	}
}
