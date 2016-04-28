/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime.translation;

import java.util.HashMap;

/**
 * Hold basic boxed data type information that can be used to recognize Java
 * boxed types and translate them to equivalent C types.
 * 
 * @author Wilson de Carvalho
 */
public class BoxedTypes {
	static private HashMap<String, String> javaAndCTypes = null;

	private static void initTypes() {
		BoxedTypes.javaAndCTypes = new HashMap<>();
		BoxedTypes.javaAndCTypes.put("Integer", "int");
		BoxedTypes.javaAndCTypes.put("Long", "long");
		BoxedTypes.javaAndCTypes.put("Float", "float");
		BoxedTypes.javaAndCTypes.put("Double", "double");
		BoxedTypes.javaAndCTypes.put("Char", "char");
		BoxedTypes.javaAndCTypes.put("Boolean", "bool");
	}

	/**
	 * Checks if a given type is a Java boxed type.
	 * 
	 * @param type
	 *            Type name.
	 * @return True if type is boxed. False otherwise.
	 */
	public static boolean isBoxed(String type) {
		if (BoxedTypes.javaAndCTypes == null)
			BoxedTypes.initTypes();
		return BoxedTypes.javaAndCTypes.containsKey(type);
	}

	/**
	 * Gets the equivalent C type for a given Java boxed type.
	 * 
	 * @param type
	 *            Java type name.
	 * @return C equivalent type. Empty if type informed is not Java boxed or is
	 *         not recognized among the boxed types available in this class.
	 */
	public static String getCType(String type) {
		if (BoxedTypes.javaAndCTypes == null)
			BoxedTypes.initTypes();
		if (!BoxedTypes.isBoxed(type))
			return "";
		else
			return BoxedTypes.javaAndCTypes.get(type);
	}
}
