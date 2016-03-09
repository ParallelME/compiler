/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import br.ufmg.dcc.parallelme.compiler.symboltable.*;

/**
 * This first pass listener is responsible for creating the symbol table.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class TranslatorFirstPassListener extends ScopeDrivenListener {
	/**
	 * Constructor.
	 * 
	 * @param rootScope
	 *            Scope that must be used as the root for scopes created during
	 *            the creation of this symbol table.
	 */
	public TranslatorFirstPassListener(Symbol rootScope) {
		super(rootScope);
	}
}