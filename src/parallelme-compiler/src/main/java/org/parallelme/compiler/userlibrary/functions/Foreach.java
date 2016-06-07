/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary.functions;

import java.util.HashMap;

import org.parallelme.compiler.userlibrary.UserLibraryClass;

/**
 * Defines the user library function class Foreach.
 * 
 * @author Wilson de Carvalho
 */
public class Foreach extends UserLibraryClass {
	private static Foreach instance = new Foreach();
	private static final String className = "Foreach";
	private static final String packageName = "org.parallelme.userlibrary.function";

	private Foreach() {
		this.initValidMethodsSet();
	}

	public static Foreach getInstance() {
		return instance;
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
	@Override
	public String getClassName() {
		return className;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPackageName() {
		return packageName;
	}
}
