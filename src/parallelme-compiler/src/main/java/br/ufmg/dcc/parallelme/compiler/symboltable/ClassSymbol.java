/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.symboltable;

/**
 * A symbol for classes' definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 * @see Symbol
 */
public class ClassSymbol extends Symbol {
	public final String typeParameterName;
	public final TokenAddress bodyAddress;

	public ClassSymbol(String name, String typeParameterName,
			Symbol enclosingScope, TokenAddress tokenAddress,
			TokenAddress bodyAddress) {
		super(name, enclosingScope, tokenAddress);
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
