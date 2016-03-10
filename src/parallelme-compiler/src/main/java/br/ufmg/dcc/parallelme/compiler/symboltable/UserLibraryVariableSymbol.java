/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.symboltable;

/**
 * A symbol for user library variables definition on the symbol table.
 * 
 * @author Wilson de Carvalho
 * @see Symbol
 */
public class UserLibraryVariableSymbol extends VariableSymbol {
	public UserLibraryVariableSymbol(String name, String typeName,
			String typeParameterName, String modifier, Symbol enclosingScope,
			TokenAddress tokenAddress, TokenAddress expressionAddress) {
		super(name, typeName, typeParameterName, modifier, enclosingScope,
				tokenAddress, expressionAddress);
	}
}
