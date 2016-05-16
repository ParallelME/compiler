/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.symboltable;

/**
 * A symbol for methods body definition on the symbol table. It will be created
 * for user library methods only.
 * 
 * @author Wilson de Carvalho
 */
public class MethodBodySymbol extends Symbol {
	public final String content;

	public MethodBodySymbol(String content, Symbol enclosingScope,
			TokenAddress tokenAddress, int identifier) {
		super("", enclosingScope, tokenAddress, identifier);
		this.content = content;
	}
}
