/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.symboltable;

/**
 * A symbol for literal definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 */
public abstract class LiteralSymbol<T> extends Symbol {
	public final T value;

	public LiteralSymbol(String name, Symbol enclosingScope, T value,
			int identifier) {
		super(name, enclosingScope, null, identifier);
		this.value = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String readableTableHeader() {
		return super.readableTableHeader() + ", " + value;
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass()) {
			LiteralSymbol<?> foo = (LiteralSymbol<?>) other;
			return super.equals(other) && foo.value == this.value;
		} else {
			return false;
		}
	}
}
