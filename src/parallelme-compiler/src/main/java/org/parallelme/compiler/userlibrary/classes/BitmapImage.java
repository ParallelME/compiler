/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary.classes;

import java.util.HashMap;

import org.parallelme.compiler.userlibrary.UserLibraryCollectionClassImpl;

/**
 * Defines the user library collection class BitmapImage.
 * 
 * @author Wilson de Carvalho
 */
public class BitmapImage extends UserLibraryCollectionClassImpl {
	private static String iteratorMethodName = "foreach";
	private static String dataOutputMethodName = "toBitmap";
	private static String getHeightName = "getHeight";
	private static String getWidthMethodName = "getWidth";
	private static BitmapImage instance = new BitmapImage();

	private BitmapImage() {
		this.initValidMethodsSet();
	}
	
	public static BitmapImage getInstance() {
		return instance;
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
		return "BitmapImage";
	}
}
