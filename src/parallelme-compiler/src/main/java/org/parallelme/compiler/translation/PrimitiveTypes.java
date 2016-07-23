/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation;

import java.util.HashMap;

/**
 * Hold basic primitive data type information that can be used to recognize Java
 * primitive types and translate them to equivalent C or JNI types.
 * 
 * @author Wilson de Carvalho
 */
public class PrimitiveTypes {
	static private HashMap<String, String[]> types = null;

	private static void initTypes() {
		// The hash map key is the Java primitive type, the first value element
		// is the equivalent C99 type and the second value is the Java
		// RenderScript type.
		types = new HashMap<>();
		types.put("int", new String[] { "int", "I32" });
		types.put("float", new String[] { "float", "F32" });
		types.put("short", new String[] { "short", "I16" });
		types.put("char", new String[] { "char", "U8" });
		types.put("boolean", new String[] { "bool", "BOOLEAN" });
		types.put("byte", new String[] { "byte", "U8" });

	}

	/**
	 * Checks if a given type is a Java primitive type.
	 * 
	 * @param type
	 *            Type name.
	 * @return True if type is primitive. False otherwise.
	 */
	public static boolean isPrimitive(String type) {
		if (types == null)
			initTypes();
		return types.containsKey(type);
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
		if (types == null)
			initTypes();
		if (!isPrimitive(type))
			return "";
		else
			return types.get(type)[0];
	}

	/**
	 * Gets the equivalent RenderScript Java type for a given Java primitive
	 * type.
	 * 
	 * @param type
	 *            Java type name.
	 * @return RenderScript Java equivalent type. Empty if type informed is not
	 *         Java primitive or is not recognized among the primitives
	 *         available in this class.
	 */
	public static String getRenderScriptJavaType(String type) {
		if (types == null)
			initTypes();
		if (!isPrimitive(type))
			return "";
		else
			return types.get(type)[1];
	}

}
