/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary.classes;

import java.util.HashSet;

import org.parallelme.compiler.userlibrary.UserLibraryCollectionClassImpl;

/**
 * Defines the user library collection class BitmapImage.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class HDRImage extends UserLibraryCollectionClassImpl {
	private static String iteratorMethodName = "foreach";
	private static String dataOutputMethodName = "toBitmap";
	private static String getHeightName = "getHeight";
	private static String getWidthMethodName = "getWidth";
	private static HDRImage instance = new HDRImage();

	private HDRImage() {
		this.initValidMethodsSet();
	}

	public static HDRImage getInstance() {
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initValidMethodsSet() {
		this.validMethods = new HashSet<>();
		this.validMethods.add(getHeightName);
		this.validMethods.add(getWidthMethodName);
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
	public String getIteratorMethodName() {
		return iteratorMethodName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDataOutputMethodName() {
		return dataOutputMethodName;
	}

	public String getHeightMethodName() {
		return getHeightName;
	}

	public String getWidthMethodName() {
		return getWidthMethodName;
	}

	/**
	 * {@inheritDoc}
	 */
	public static String getName() {
		return "HDRImage";
	}
}
