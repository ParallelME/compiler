/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary;

import org.parallelme.compiler.userlibrary.classes.*;
import org.parallelme.compiler.userlibrary.functions.*;

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
			return BitmapImage.getInstance();
		else if (className.equals(HDRImage.getName()))
			return HDRImage.getInstance();
		else if (className.equals(Foreach.getName()))
			return new Foreach();
		else if (className.equals(Reduce.getName()))
			return new Reduce();
		else if (className.equals(RGB.getName()))
			return RGB.getInstance();
		else if (className.equals(RGBA.getName()))
			return RGBA.getInstance();
		else if (className.equals(Pixel.getName()))
			return Pixel.getInstance();
		else if (className.equals(Int16.getName()))
			return Int16.getInstance();
		else if (className.equals(Int32.getName()))
			return Int32.getInstance();
		else if (className.equals(Float32.getName()))
			return Float32.getInstance();
		else if (className.equals(Array.getName()))
			return Array.getInstance();
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
