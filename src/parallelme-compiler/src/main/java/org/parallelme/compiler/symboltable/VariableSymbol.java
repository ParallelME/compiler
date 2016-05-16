/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.symboltable;

/**
 * A symbol for variables definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 */
public class VariableSymbol extends Symbol {
	public final String typeName;
	public final String typeParameterName;
	public final String modifier;
	public final TokenAddress statementAddress;

	public VariableSymbol(String name, String typeName,
			String typeParameterName, String modifier, Symbol enclosingScope,
			TokenAddress tokenAddress, TokenAddress statementAddress,
			int identifier) {
		super(name, enclosingScope, tokenAddress, identifier);
		this.typeName = typeName;
		this.typeParameterName = typeParameterName;
		this.modifier = modifier;
		this.statementAddress = statementAddress;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String readableTableHeader() {
		return super.readableTableHeader() + ", " + typeName + ", "
				+ typeParameterName + ", " + modifier;
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass()) {
			VariableSymbol foo = (VariableSymbol) other;
			return super.equals(other) && foo.typeName == this.typeName
					&& foo.typeParameterName == this.typeParameterName
					&& foo.modifier == this.modifier
					&& foo.statementAddress == this.statementAddress;
		} else {
			return false;
		}
	}
}
