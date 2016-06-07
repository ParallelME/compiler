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
 * Base class for all user library classes' implementations.
 * 
 * @author Wilson de Carvalho
 */
public abstract class UserLibraryClass {
	protected Map<String, String> validMethods;

	abstract protected void initValidMethodsSet();

	/**
	 * Indicates if this class was designed to work with type arguments.
	 * 
	 * @return True if this class was designed to work with type arguments.
	 */
	abstract public boolean isTyped();

	/**
	 * List all valid methods for this class, EXCEPT output bind and operation
	 * methods. These last methods names must be returned in specific methods.
	 * 
	 * @return A Set with valid method names.
	 */
	public Set<String> getValidMethods() {
		return this.validMethods.keySet();
	}

	/**
	 * Get the return type of a valid method (non-operation and non-output
	 * bind).
	 */
	public String getReturnType(String methodName) {
		if (this.isValidMethod(methodName))
			return this.validMethods.get(methodName);
		else
			return "";
	}

	/**
	 * Indicates if the method name provided is valid for this user library
	 * class.
	 */
	public boolean isValidMethod(String methodName) {
		return this.validMethods.containsKey(methodName);
	}

	/**
	 * Gets class name.
	 * 
	 * @return Class name.
	 */
	abstract public String getClassName();

	/**
	 * Gets package name.
	 * 
	 * @return Package name.
	 */
	abstract public String getPackageName();

	/**
	 * Gets class fully qualified name (package + class names).
	 * 
	 * @return Fully qualified name.
	 */
	public String getFullyQualifiedName() {
		return this.getPackageName() + "." + this.getClassName();
	}	
}
