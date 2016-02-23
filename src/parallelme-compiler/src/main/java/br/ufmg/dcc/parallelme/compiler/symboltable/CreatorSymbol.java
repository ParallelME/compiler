/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.symboltable;

import java.util.Collection;

/**
 * A symbol for variable creation definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 * @see Symbol
 */
public class CreatorSymbol extends Symbol {
	public final Collection<Symbol> arguments;
	// Creator class type
	public final String typeName;
	// Type parametrized on the creator
	public final String typeParameterName;

	public CreatorSymbol(String name, String typeName,
			String typeParameterName, Collection<Symbol> arguments,
			Symbol enclosingScope, TokenAddress tokenAddress) {
		super(name, enclosingScope, tokenAddress);
		this.typeName = typeName;
		this.typeParameterName = typeParameterName;
		this.arguments = arguments;
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
				+ this.typeParameterName + ", " + ret.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass()) {
			CreatorSymbol foo = (CreatorSymbol) other;
			return super.equals(other) && foo.arguments == this.arguments
					&& foo.typeName == this.typeName
					&& foo.typeParameterName == this.typeParameterName;
		} else {
			return false;
		}
	}
}
