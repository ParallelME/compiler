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
 * Defines the output bind data that must be passed to the runtime to translate
 * the code.
 * 
 * @author Wilson de Carvalho
 */
public class OutputBind extends UserLibraryData {
	private Variable destinationObject;
	private TokenAddress statementAddress;

	public OutputBind(Variable variable, Variable destinationObject,
			int sequentialNumber, TokenAddress statementAddress) {
		super(variable, sequentialNumber);
		this.setDestinationObject(destinationObject);
		this.setStatementAddress(statementAddress);
	}

	public Variable getDestinationObject() {
		return destinationObject;
	}

	public void setDestinationObject(Variable destinationObject) {
		this.destinationObject = destinationObject;
	}

	public TokenAddress getStatementAddress() {
		return statementAddress;
	}

	public void setStatementAddress(TokenAddress statementAddress) {
		this.statementAddress = statementAddress;
	}
}
