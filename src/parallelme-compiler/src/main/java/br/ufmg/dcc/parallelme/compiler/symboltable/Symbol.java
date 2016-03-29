/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.symboltable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract symbol definition for scope-drive symbol table creation. It must be
 * inherited to define proper symbol types.
 * 
 * @author Wilson de Carvalho
 */
public abstract class Symbol {
	public final LinkedHashMap<String, Symbol> innerSymbols;

	public final String name;
	public final Symbol enclosingScope;
	public final TokenAddress tokenAddress;

	public Symbol(String name, Symbol enclosingScope, TokenAddress tokenAddress) {
		this.innerSymbols = new LinkedHashMap<>();
		this.name = name;
		this.enclosingScope = enclosingScope;
		this.tokenAddress = tokenAddress;
	}

	/**
	 * Search for a valid symbol under this scope, including in the search those
	 * symbols in the enclosing scopes (recursively).
	 * 
	 * Cost:
	 * 
	 * Omega(1) (best case); O(n) (worst case). Where <b>n</b> is the number of
	 * enclosing scopes.
	 * 
	 * @param name
	 *            Name of the symbol being searched.
	 * @return Symbol object if found, null otherwise.
	 */
	public Symbol getSymbolUnderScope(String name) {
		if (this.innerSymbols.containsKey(name))
			return this.innerSymbols.get(name);
		else if (this.enclosingScope != null)
			return this.enclosingScope.getSymbolUnderScope(name);
		else
			return null;
	}

	/**
	 * Search for a valid symbol under this scope, including in the search those
	 * symbols in the enclosing scopes (recursively).
	 * 
	 * Cost:
	 * 
	 * Omega(1) (best case); O(n) (worst case). Where <b>n</b> is the number of
	 * enclosing scopes.
	 * 
	 * @param name
	 *            Name of the symbol being searched.
	 * @param symbolClass
	 *            Class of the symbol being searched.
	 * 
	 * @return Symbol object if found, null otherwise.
	 */
	public Symbol getSymbolUnderScope(String name, Class<?> symbolClass) {
		if (this.innerSymbols.containsKey(name)
				&& this.innerSymbols.get(name).getClass() == symbolClass)
			return this.innerSymbols.get(name);
		else if (this.enclosingScope != null)
			return this.enclosingScope.getSymbolUnderScope(name, symbolClass);
		else
			return null;
	}

	/**
	 * Search for all symbols of a provided class under this scope.
	 * 
	 * @param symbolClass
	 *            The symbol class that is being searched.
	 * 
	 * @return A map of symbols with with their names as keys.
	 */
	public Map<String, Symbol> getSymbolsUnderScope(Class<?> symbolClass) {
		HashMap<String, Symbol> ret = new HashMap<>();
		for (Symbol symbol : this.innerSymbols.values()) {
			if (symbol.getClass() == symbolClass)
				ret.put(symbol.name, symbol);
		}
		if (this.enclosingScope != null)
			ret.putAll(this.enclosingScope.getSymbolsUnderScope(symbolClass));
		return ret;
	}

	/**
	 * Checks the existence of a symbol that is valid under this scope,
	 * including in the search those symbols in the enclosing scopes
	 * (recursively).
	 * 
	 * @param name
	 *            Name of the symbol being searched.
	 * @return True if found, false otherwise.
	 */
	public boolean hasSymbolUnderScope(String name) {
		return this.getSymbolUnderScope(name) != null;
	}

	/**
	 * Search for a valid symbol among the inner symbols.
	 * 
	 * @param name
	 *            Name of the symbol being searched.
	 * @return Symbol object if found, null otherwise.
	 */
	public Symbol getInnerSymbol(String name) {
		Symbol symbol = null;
		if (this.innerSymbols.containsKey(name)) {
			symbol = this.innerSymbols.get(name);
		}
		return symbol;
	}

	/**
	 * Search for a valid symbol among the inner symbols.
	 * 
	 * @param name
	 *            Name of the symbol being searched.
	 * @param symbolClass
	 *            The symbol class that is being searched.
	 * 
	 * @return Symbol object if found, null otherwise.
	 */
	public Symbol getInnerSymbol(String name, Class<?> symbolClass) {
		Symbol symbol = null;
		if (this.innerSymbols.containsKey(name)) {
			if (this.innerSymbols.get(name).getClass() == symbolClass) {
				symbol = this.innerSymbols.get(name);
			}
		}
		return symbol;
	}

	/**
	 * Search for all symbols of a provided class under this scope and among the
	 * scope of the inner symbols.
	 * 
	 * @param symbolClass
	 *            The symbol class that is being searched.
	 * 
	 * @return An array of symbols of the informed class.
	 */
	public ArrayList<Symbol> getSymbols(Class<?> symbolClass) {
		ArrayList<Symbol> ret = new ArrayList<>();
		for (Symbol symbol : this.innerSymbols.values()) {
			if (symbol.getClass() == symbolClass)
				ret.add(symbol);
			if (symbol.innerSymbols != null)
				ret.addAll(symbol.getSymbols(symbolClass));
		}
		return ret;
	}

	/**
	 * Search for a valid class symbol under this scope, including in the search
	 * those symbols in the enclosing scopes (recursively).
	 * 
	 * @return ClassSymbol object if found, null otherwise.
	 */
	public ClassSymbol getEnclosingClass() {
		if (this.enclosingScope != null) {
			if (this.enclosingScope.getClass() == ClassSymbol.class)
				return (ClassSymbol) this.enclosingScope;
			else
				return this.enclosingScope.getEnclosingClass();
		} else {
			return null;
		}
	}

	public void addSymbol(Symbol symbol) {
		this.innerSymbols.put(symbol.name, symbol);
	}

	@Override
	public String toString() {
		return this.readableTable("").toString();
	}

	/**
	 * Transforms this table into a hierarchical readable string.
	 * 
	 * @param space
	 *            Space that will be used to indicate hierarchical dependency
	 *            among levels.
	 * @return A StringBuffer object with a formatted string.
	 */
	private StringBuffer readableTable(String space) {
		StringBuffer ret = new StringBuffer();
		ret.append(space + this.readableTableHeader());
		for (Symbol symbol : this.innerSymbols.values())
			ret.append("\n" + symbol.readableTable(space + "  "));
		return ret;
	}

	/**
	 * Find the hierarchical root of this symbol table.
	 * 
	 * @return Root object.
	 */
	public Symbol getRoot() {
		if (this.enclosingScope == null)
			return this;
		else
			return this.enclosingScope.getRoot();
	}

	/**
	 * Return a string that represents this symbol to be used on the readable
	 * table header.
	 * 
	 * @return String consisting of basic information about this symbol.
	 */
	protected String readableTableHeader() {
		return "<" + getClass().getSimpleName() + "> " + name;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Symbol) {
			Symbol foo = (Symbol) other;
			return this.innerSymbols == foo.innerSymbols
					&& this.name == foo.name
					&& this.enclosingScope == foo.enclosingScope
					&& this.tokenAddress == foo.tokenAddress;
		} else {
			return false;
		}
	}
}
