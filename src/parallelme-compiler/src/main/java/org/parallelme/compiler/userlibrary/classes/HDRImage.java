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
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class HDRImage extends Image {
	private static HDRImage instance = new HDRImage();
	private static final String className = "HDRImage";

	private HDRImage() {
		super();
	}

	public static HDRImage getInstance() {
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
