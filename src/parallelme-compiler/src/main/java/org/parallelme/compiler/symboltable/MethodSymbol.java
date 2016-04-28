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
 * A symbol for methods' definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 */
public class MethodSymbol extends Symbol {
	public final String returnType;
	public final Collection<Symbol> arguments;

	public MethodSymbol(String name, String returnType,
			Collection<Symbol> arguments, Symbol enclosingScope,
			TokenAddress tokenAddress) {
		super(name, enclosingScope, tokenAddress);
		this.returnType = returnType;
		this.arguments = arguments;
		for (Symbol argument : arguments)
			this.addSymbol(argument);
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
		return super.readableTableHeader() + ", " + returnType + ", "
				+ ret.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass()) {
			MethodSymbol foo = (MethodSymbol) other;
			return super.equals(other) && foo.returnType == this.returnType
					&& foo.arguments == this.arguments;
		} else {
			return false;
		}
	}
}
