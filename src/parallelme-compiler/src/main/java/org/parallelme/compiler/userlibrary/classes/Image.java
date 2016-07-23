/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary.classes;

import java.util.HashMap;

import org.parallelme.compiler.userlibrary.UserLibraryCollection;

/**
 * Defines user library Image derived classes.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public abstract class Image extends UserLibraryCollection {
	private static String outputBindMethodName = "toBitmap";
	private static String getHeightMethodName = "getHeight";
	private static String getWidthMethodName = "getWidth";
	private static final String packageName = "org.parallelme.userlibrary.image";

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
		this.validMethods.put(getHeightMethodName, "int");
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
		return getHeightMethodName;
	}

	public String getWidthMethodName() {
		return getWidthMethodName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPackageName() {
		return packageName;
	}
}
