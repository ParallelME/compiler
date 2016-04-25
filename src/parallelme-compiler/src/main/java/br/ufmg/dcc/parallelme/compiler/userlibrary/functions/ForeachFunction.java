/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.userlibrary.functions;

import java.util.HashSet;

import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassImpl;

/**
 * Defines the user library function class ForeachFunction.
 * 
 * @author Wilson de Carvalho
 */
public class ForeachFunction extends UserLibraryClassImpl {
	public ForeachFunction() {
		this.initValidMethodsSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initValidMethodsSet() {
		this.validMethods = new HashSet<>();
		this.validMethods.add("function");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTyped() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public static String getName() {
		return "ForeachFunction";
	}
}
