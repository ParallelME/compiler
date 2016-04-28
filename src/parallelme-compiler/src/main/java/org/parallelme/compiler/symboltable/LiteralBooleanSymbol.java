/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.symboltable;

/**
 * A symbol for string character definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 */
public class LiteralBooleanSymbol extends LiteralSymbol<Boolean> {
	public LiteralBooleanSymbol(String name, Symbol enclosingScope, Boolean value) {
		super(name, enclosingScope, value);
	}
}
