/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary.classes;

/**
 * Defines the user library collection class BitmapImage.
 * 
 * @author Wilson de Carvalho
 */
public class BitmapImage extends Image {
	private static BitmapImage instance = new BitmapImage();
	private static final String className = "BitmapImage";

	private BitmapImage() {
		super();
	}

	public static BitmapImage getInstance() {
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClassName() {
		return className;
	}
}
