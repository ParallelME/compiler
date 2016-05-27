/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary;

import java.util.Set;

/**
 * Describes a User Library class.
 * 
 * This interface and derived classes were solely created to accomplish a first
 * viable version (prototype) of our compiler. Thus in future version we should
 * eliminate explicit user library classes' declaration and move to a
 * parser-based approach in which the compiler would also analyze user library
 * classes in order to evaluate parameters, methods, constructors and so on.
 * 
 * @author Wilson de Carvalho
 */
public interface UserLibraryClass {
	/**
	 * Indicates if this class was designed to work with type arguments.
	 * 
	 * @return True if this class was designed to work with type arguments.
	 */
	public boolean isTyped();

	/**
	 * List all valid methods for this class, EXCEPT output bind and operation
	 * methods. These last methods names must be returned in specific methods.
	 * 
	 * @return A Set with valid method names.
	 */
	public Set<String> getValidMethods();

	/**
	 * Get the return type of a valid method (non-operation and non-output bind).
	 */
	public String getReturnType(String methodName);

	/**
	 * Indicates if the method name provided is valid for this user library
	 * class.
	 * 
	 * @return True if the method is valid and false otherwise.
	 */
	public boolean isValidMethod(String methodName);
}
