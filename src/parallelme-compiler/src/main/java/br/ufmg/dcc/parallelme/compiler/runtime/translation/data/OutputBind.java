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
	private TokenAddress expressionAddress;

	public OutputBind(Variable variable, Variable destinationObject,
			int sequentialNumber, TokenAddress expressionAddress) {
		super(variable, sequentialNumber);
		this.setDestinationObject(destinationObject);
		this.setExpressionAddress(expressionAddress);
	}

	public Variable getDestinationObject() {
		return destinationObject;
	}

	public void setDestinationObject(Variable destinationObject) {
		this.destinationObject = destinationObject;
	}

	public TokenAddress getExpressionAddress() {
		return expressionAddress;
	}

	public void setExpressionAddress(TokenAddress expressionAddress) {
		this.expressionAddress = expressionAddress;
	}

	@Override
	public int hashCode() {
		return this.destinationObject.hashCode() * 3
				+ this.expressionAddress.hashCode() * 7;
	}
}
