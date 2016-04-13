/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.symboltable;

/**
 * A symbol for methods body definition on the symbol table. It will be created
 * for user library methods only.
 * 
 * @author Wilson de Carvalho
 */
public class MethodBodySymbol extends Symbol {
	public final String content;

	public MethodBodySymbol(String content, Symbol enclosingScope,
			TokenAddress tokenAddress) {
		super("", enclosingScope, tokenAddress);
		this.content = content;
	}
}
