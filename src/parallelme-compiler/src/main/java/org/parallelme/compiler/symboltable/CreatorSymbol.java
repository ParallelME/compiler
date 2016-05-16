/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.symboltable;

import java.util.Collection;

/**
 * A symbol for variable creation definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 */
public class CreatorSymbol extends Symbol {
	public final Collection<Symbol> arguments;
	// Creator class type
	public final String typeName;
	// Attributed object name (if any)
	public final String attributedObjectName;
	// Type parametrized on the creator
	public final String typeParameterName;
	public final TokenAddress statementAddress;

	public CreatorSymbol(String name, String attributedObjectName,
			String typeName, String typeParameterName,
			Collection<Symbol> arguments, Symbol enclosingScope,
			TokenAddress tokenAddress, TokenAddress statementAddress,
			int identifier) {
		super(name, enclosingScope, tokenAddress, identifier);
		this.typeName = typeName;
		this.attributedObjectName = attributedObjectName;
		this.typeParameterName = typeParameterName;
		this.arguments = arguments;
		this.statementAddress = statementAddress;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String readableTableHeader() {
		StringBuffer ret = new StringBuffer();
		ret.append("[");
		for (Symbol symbol : this.arguments)
			ret.append(symbol + "; ");
		if (ret.length() > 1)
			ret.delete(ret.length() - 2, ret.length());
		ret.append("]");
		return super.readableTableHeader() + ", " + this.typeName + ", "
				+ attributedObjectName + ", " + this.typeParameterName + ", "
				+ ret.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass()) {
			CreatorSymbol foo = (CreatorSymbol) other;
			return super.equals(other) && foo.arguments == this.arguments
					&& foo.typeName == this.typeName
					&& foo.typeParameterName == this.typeParameterName
					&& foo.statementAddress == this.statementAddress;
		} else {
			return false;
		}
	}
}
