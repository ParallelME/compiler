/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary.functions;

import java.util.HashMap;

import org.parallelme.compiler.userlibrary.UserLibraryClassImpl;

/**
 * Defines the user library function class Foreach.
 * 
 * @author Wilson de Carvalho
 */
public class Foreach extends UserLibraryClassImpl {
	public Foreach() {
		this.initValidMethodsSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initValidMethodsSet() {
		this.validMethods = new HashMap<>();
		this.validMethods.put("function", "void");
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
		return "Foreach";
	}
}
