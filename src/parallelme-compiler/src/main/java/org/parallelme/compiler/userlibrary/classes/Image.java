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
 * Defines user library Image derived classes.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public abstract class Image extends UserLibraryCollectionClass {
	private static String outputBindMethodName = "toBitmap";
	private static String getHeightName = "getHeight";
	private static String getWidthMethodName = "getWidth";

	public Image() {
		super();
		this.initValidMethodsSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initValidMethodsSet() {
		this.validMethods = new HashMap<>();
		this.validMethods.put(getHeightName, "int");
		this.validMethods.put(getWidthMethodName, "int");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTyped() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOutputBindMethodName() {
		return outputBindMethodName;
	}

	public String getHeightMethodName() {
		return getHeightName;
	}

	public String getWidthMethodName() {
		return getWidthMethodName;
	}
}
