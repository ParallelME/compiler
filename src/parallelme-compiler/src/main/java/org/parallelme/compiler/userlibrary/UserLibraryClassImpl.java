/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary;

import java.util.Map;
import java.util.Set;

/**
 * Several useful implementations for derived concrete classes that will
 * represent user library classes.
 * 
 * @author Wilson de Carvalho
 */
public abstract class UserLibraryClassImpl implements UserLibraryClass {
	protected Map<String, String> validMethods;

	protected abstract void initValidMethodsSet();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getValidMethods() {
		return this.validMethods.keySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getReturnType(String methodName) {
		if (this.isValidMethod(methodName))
			return this.validMethods.get(methodName);
		else
			return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidMethod(String methodName) {
		return this.validMethods.containsKey(methodName);
	}
}
