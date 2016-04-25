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
 * A symbol for expressions that must not have all its componentes recognized
 * separately by the compiler. These symbols will be treated as an unique piece
 * of code.
 * 
 * @author Wilson de Carvalho
 */
public class ExpressionSymbol extends Symbol {
	public ExpressionSymbol(String name, Symbol enclosingScope,
			TokenAddress tokenAddress) {
		super(name, enclosingScope, tokenAddress);
	}
}
