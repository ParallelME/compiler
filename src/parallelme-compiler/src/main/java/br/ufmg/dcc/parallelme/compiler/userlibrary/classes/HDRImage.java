/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.userlibrary.classes;

import java.util.HashSet;

import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryCollectionClassImpl;

/**
 * Defines the user library collection class BitmapImage.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class HDRImage extends UserLibraryCollectionClassImpl {
	public HDRImage() {
		this.initValidMethodsSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initValidMethodsSet() {
		this.validMethods = new HashSet<>();
		this.validMethods.add("toBitmap");
		this.validMethods.add("foreach");
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
		return "foreach";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDataOutputMethodName() {
		return "toBitmap";
	}

	/**
	 * {@inheritDoc}
	 */
	public static String getName() {
		return "HDRImage";
	}
}
