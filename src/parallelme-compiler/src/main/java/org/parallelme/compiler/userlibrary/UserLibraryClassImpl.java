/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary;

import java.util.HashSet;

/**
 * Several useful implementations for derived concrete classes that will
 * represent user library classes.
 * 
 * @author Wilson de Carvalho
 */
public abstract class UserLibraryClassImpl implements UserLibraryClass {
	protected HashSet<String> validMethods;

	protected abstract void initValidMethodsSet();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HashSet<String> getValidMethods() {
		return this.validMethods;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidMethod(String methodName) {
		return this.validMethods.contains(methodName);
	}
}
