/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.userlibrary;

import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.functions.*;

/**
 * Generic user library class factory.
 * 
 * @author Wilson de Carvalho
 */
public class UserLibraryClassFactory {
	/**
	 * Create a user library class object.
	 * 
	 * @param className
	 *            Name of the desired class.
	 * @return User librar object if the class name provided is valid. False
	 *         otherwise.
	 */
	public static UserLibraryClass create(String className) {
		if (className.equals(BitmapImage.getName()))
			return new BitmapImage();
		else if (className.equals(HDRImage.getName()))
			return new HDRImage();
		else if (className.equals(UserFunction.getName()))
			return new UserFunction();
		else if (className.equals(RGB.getName()))
			return new RGB();
		else if (className.equals(RGBA.getName()))
			return new RGBA();
		else if (className.equals(Pixel.getName()))
			return new Pixel();
		else
			return null;
	}

	/**
	 * Checks if a given class name corresponds to a valid user library class.
	 * 
	 * @param className
	 *            Name of the desired class.
	 * @return True if valid. False otherwise.
	 */
	public static boolean isValidClass(String className) {
		return UserLibraryClassFactory.create(className) != null;
	}

	/**
	 * Checks if the given class name corresponds to a typed user library class.
	 * 
	 * @param className
	 *            Name of the desired class.
	 * @return True if typed. False otherwise.
	 */
	public static boolean isTyped(String className) {
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.create(className);
		if (userLibraryClass != null) {
			return userLibraryClass.isTyped();
		} else {
			return false;
		}
	}
}
