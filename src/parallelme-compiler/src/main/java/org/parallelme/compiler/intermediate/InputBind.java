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
 * Intermediate representation for input bind operations.
 * 
 * @author Wilson de Carvalho
 */
public class InputBind extends UserLibraryData {
	public final Parameter[] parameters;
	public final TokenAddress declarationStatementAddress;
	public final TokenAddress creationStatementAddress;

	public InputBind(Variable variable, int sequentialNumber,
			Parameter[] parameters, TokenAddress declarationStatementAddress,
			TokenAddress creationStatementAddress) {
		super(variable, sequentialNumber);
		this.parameters = parameters;
		this.declarationStatementAddress = declarationStatementAddress;
		this.creationStatementAddress = creationStatementAddress;
	}
}
