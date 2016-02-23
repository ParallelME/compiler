/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import java.util.ArrayList;
import java.util.Collection;

import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser;
import br.ufmg.dcc.parallelme.compiler.symboltable.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.*;

/**
 * This first pass listener is responsible for creating the symbol table and
 * list all those imports from user library classes that must be removed.
 * 
 * @author Wilson de Carvalho
 */
public class TranslatorFirstPassListener extends ScopeDrivenListener {
	// List of those tokens that must be removed from the output code.
	private final ArrayList<TokenAddress> importTokens = new ArrayList<TokenAddress>();

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

	/**
	 * List user library import statements.
	 * 
	 * @return A collection of token addresses.
	 */
	public Collection<TokenAddress> getImportTokens() {
		return this.importTokens;
	}
	
	/**
	 * Insert those import declarations that contains ParallelME packages in the
	 * list of imports that must be removed from the output code.
	 * 
	 * @param ctx
	 *            Import context.
	 */
	@Override
	public void enterImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
		String importDeclaration = ctx.qualifiedName().getText();
		if (importDeclaration.contains(PackageDefinition.getBasePackage())) {
			this.importTokens.add(new TokenAddress(ctx.start, ctx.stop));
		}
	}
}