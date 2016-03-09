/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import br.ufmg.dcc.parallelme.compiler.symboltable.Symbol;

/**
 * Build the symbol table and translate imports.
 * 
 * @author Wilson de Carvalho
 */
public class TranslatorFirstPass {
	public TranslatorFirstPass() {
	}

	public void run(TokenStreamRewriter tokenStreamRewriter,
			Symbol symbolTable, ParseTree tree) {
		TranslatorFirstPassListener listener = new TranslatorFirstPassListener(
				symbolTable);
		ParseTreeWalker walker = new ParseTreeWalker();
		// 1. Walk on the parse tree
		walker.walk(listener, tree);
	}
}