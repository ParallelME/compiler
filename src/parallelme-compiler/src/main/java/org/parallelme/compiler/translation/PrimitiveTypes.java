/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Hold basic primitive data type information that can be used to recognize Java
 * primitive types and translate them to equivalent C or JNI types.
 * 
 * @author Wilson de Carvalho
 */
public class PrimitiveTypes {
	static private HashMap<String, String> cTypes = null;
	static private HashSet<String> numericTypes = null;

	private static void initTypes() {
		cTypes = new HashMap<>();
		numericTypes = new HashSet<>();

		numericTypes.add("int");
		numericTypes.add("float");
		numericTypes.add("short");

		cTypes.put("int", "int");
		cTypes.put("float", "float");
		cTypes.put("short", "short");
		cTypes.put("char", "char");
		cTypes.put("boolean", "bool");
		cTypes.put("byte", "byte");
	}

	/**
	 * Checks if a given type is a Java primitive type.
	 * 
	 * @param type
	 *            Type name.
	 * @return True if type is primitive. False otherwise.
	 */
	public static boolean isPrimitive(String type) {
		if (cTypes == null)
			initTypes();
		return cTypes.containsKey(type);
	}

	/**
	 * Gets the equivalent C type for a given Java primitive type.
	 * 
	 * @param type
	 *            Java type name.
	 * @return C equivalent type. Empty if type informed is not Java primitive
	 *         or is not recognized among the primitives available in this
	 *         class.
	 */
	public static String getCType(String type) {
		if (cTypes == null)
			initTypes();
		if (!isPrimitive(type))
			return "";
		else
			return cTypes.get(type);
	}
}
