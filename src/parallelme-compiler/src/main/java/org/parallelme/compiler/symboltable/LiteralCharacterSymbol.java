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
public class LiteralCharacterSymbol extends LiteralSymbol<Character> {
	public LiteralCharacterSymbol(String name, Symbol enclosingScope,
			Character value, int identifier) {
		super(name, enclosingScope, value, identifier);
	}
}
