/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.BitmapImage;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.HDRImage;

/**
 * Stores common definitions used during C and H files creation.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeCFileBaseImpl {
	/**
	 * Return a library name for a given type.
	 * 
	 * @param typeName
	 *            Type name.
	 * @return Library name for specified type.
	 */
	protected String getImportLibrary(String typeName) {
		if (typeName.equals(HDRImage.getName())
				|| typeName.equals(BitmapImage.getName())) {
			return "<android/bitmap.h>";
		}
		return null;
	}
}
