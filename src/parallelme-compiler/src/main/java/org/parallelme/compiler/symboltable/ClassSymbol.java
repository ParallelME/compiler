/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.symboltable;

/**
 * A symbol for classes' definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 */
public class ClassSymbol extends Symbol {
	public final String typeParameterName;
	public final TokenAddress bodyAddress;

	public ClassSymbol(String name, String typeParameterName,
			Symbol enclosingScope, TokenAddress tokenAddress,
			TokenAddress bodyAddress, int identifier) {
		super(name, enclosingScope, tokenAddress, identifier);
		this.typeParameterName = typeParameterName;
		this.bodyAddress = bodyAddress;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String readableTableHeader() {
		return super.readableTableHeader() + ", " + typeParameterName;
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass()) {
			ClassSymbol foo = (ClassSymbol) other;
			return super.equals(other)
					&& this.typeParameterName == foo.typeParameterName;
		} else {
			return false;
		}
	}
}
