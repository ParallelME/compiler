/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary.classes;

import java.util.HashMap;

import org.parallelme.compiler.userlibrary.UserLibraryCollectionClass;

/**
 * Defines the user library collection class Array.
 * 
 * @author Wilson de Carvalho
 */
public class Array extends UserLibraryCollectionClass {
	private static String outputBindMethodName = "toJavaArray";
	private static Array instance = new Array();

	private Array() {
		this.initValidMethodsSet();
	}
	
	public static Array getInstance() {
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initValidMethodsSet() {
		this.validMethods = new HashMap<>();
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
	public String getOutputBindMethodName() {
		return outputBindMethodName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public static String getName() {
		return "Array";
	}
}
