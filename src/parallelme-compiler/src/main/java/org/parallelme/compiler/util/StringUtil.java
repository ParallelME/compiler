/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.util;

public class StringUtil {
	public static String mkString(Object[] array, String separator) {
		StringBuilder ret = new StringBuilder();
		if (array.length > 0) {
			for (int i = 0; i < array.length - 1; i++) {
				ret.append(array[i].toString());
				ret.append(separator);
			}
			ret.append(array[array.length - 1].toString());
		}
		return ret.toString();
	}
}
