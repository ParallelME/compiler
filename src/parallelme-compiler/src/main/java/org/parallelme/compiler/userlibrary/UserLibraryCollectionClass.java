/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary;

import java.util.HashSet;
import java.util.Set;

/**
 * Several useful implementations for derived concrete classes that will
 * represent user library collection classes.
 * 
 * @author Wilson de Carvalho
 */
public abstract class UserLibraryCollectionClass extends UserLibraryClassImpl {
	private static String foreachMethodName = "foreach";
	private static String reduceMethodName = "reduce";
	private static Set<String> operationMethods;

	static {
		operationMethods = new HashSet<>();
		operationMethods.add(foreachMethodName);
		operationMethods.add(reduceMethodName);
	}

	public static Set<String> getOperationMethods() {
		return operationMethods;
	}

	/**
	 * Gets foreach method name.
	 * 
	 * @return Method name.
	 */
	public static String getForeachMethodName() {
		return foreachMethodName;
	}

	/**
	 * Gets reduce method name.
	 * 
	 * @return Reduce name.
	 */
	public static String getReduceMethodName() {
		return reduceMethodName;
	}

	/**
	 * Gets output bind method name.
	 * 
	 * @return Method name.
	 */
	abstract public String getOutputBindMethodName();
}
