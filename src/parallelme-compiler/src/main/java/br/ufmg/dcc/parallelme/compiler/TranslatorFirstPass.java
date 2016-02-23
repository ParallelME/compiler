/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import java.util.Collection;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import br.ufmg.dcc.parallelme.compiler.runtime.RuntimeDefinition;
import br.ufmg.dcc.parallelme.compiler.symboltable.ClassSymbol;
import br.ufmg.dcc.parallelme.compiler.symboltable.Symbol;
import br.ufmg.dcc.parallelme.compiler.symboltable.TokenAddress;

/**
 * Build the symbol table and translate imports.
 * 
 * @author Wilson de Carvalho
 */
public class TranslatorFirstPass {
	private final RuntimeDefinition runtime;
	private int iteratorsCount = 0;

	public TranslatorFirstPass(RuntimeDefinition runtime) {
		this.runtime = runtime;
	}

	public int getIteratorsCount() {
		return iteratorsCount;
	}

	public void run(TokenStreamRewriter tokenStreamRewriter,
			Symbol symbolTable, ParseTree tree, int lastIteratorCount) {
		TranslatorFirstPassListener listener = new TranslatorFirstPassListener(
				symbolTable);
		ParseTreeWalker walker = new ParseTreeWalker();
		// 1. Walk on the parse tree
		walker.walk(listener, tree);
		// 2. Remove user library imports (if any)
		this.removeUserLibraryImports(tokenStreamRewriter,
				listener.getImportTokens());
		// 3. If user library classes were detected, we must insert the runtime
		// imports
		if (listener.getUserLibraryDetected()) {
			Collection<Symbol> classSymbols = symbolTable
					.getSymbols(ClassSymbol.class);
			if (!classSymbols.isEmpty()) {
				ClassSymbol classSymbolTable = (ClassSymbol) classSymbols
						.iterator().next();
				// Insert runtime imports
				this.insertRuntimeImports(tokenStreamRewriter, classSymbolTable);
			}
		}
	}

	/**
	 * Remove user library imports.
	 */
	private void removeUserLibraryImports(
			TokenStreamRewriter tokenStreamRewriter,
			Collection<TokenAddress> importTokens) {
		for (TokenAddress importToken : importTokens) {
			tokenStreamRewriter.delete(importToken.start, importToken.stop);
		}
	}

	/**
	 * Insert necessary imports for the runtime usage.
	 */
	private void insertRuntimeImports(TokenStreamRewriter tokenStreamRewriter,
			Symbol classTable) {
		tokenStreamRewriter.insertBefore(classTable.tokenAddress.start,
				this.runtime.getImports());
	}
}