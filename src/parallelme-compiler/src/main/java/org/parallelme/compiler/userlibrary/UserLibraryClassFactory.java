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
	 * Returns an user library class object.
	 * 
	 * @param className
	 *            Name of the desired class.
	 * @return User library object if the class name provided is valid. Null
	 *         otherwise.
	 */
	public static UserLibraryClass getClass(String className) {
		if (className.equals(BitmapImage.getInstance().getClassName()))
			return BitmapImage.getInstance();
		else if (className.equals(HDRImage.getInstance().getClassName()))
			return HDRImage.getInstance();
		else if (className.equals(Foreach.getInstance().getClassName()))
			return Foreach.getInstance();
		else if (className.equals(Reduce.getInstance().getClassName()))
			return Reduce.getInstance();
		else if (className.equals(Pixel.getInstance().getClassName()))
			return Pixel.getInstance();
		else if (className.equals(Int16.getInstance().getClassName()))
			return Int16.getInstance();
		else if (className.equals(Int32.getInstance().getClassName()))
			return Int32.getInstance();
		else if (className.equals(Float32.getInstance().getClassName()))
			return Float32.getInstance();
		else if (className.equals(Array.getInstance().getClassName()))
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
		return UserLibraryClassFactory.getClass(className) != null;
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
				.getClass(className);
		if (userLibraryClass != null) {
			return userLibraryClass.isTyped();
		} else {
			return false;
		}
	}
}
