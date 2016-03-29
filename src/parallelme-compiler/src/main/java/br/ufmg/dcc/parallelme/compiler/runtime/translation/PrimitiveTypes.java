/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime.translation;

import java.util.HashMap;

import br.ufmg.dcc.parallelme.compiler.util.Pair;

/**
 * Hold basic primitive data type information that can be used to recognize Java
 * primitive types and translate them to equivalent C or JNI types.
 * 
 * @author Wilson de Carvalho
 */
public class PrimitiveTypes {
	// Key: java primitive type. Value: Pair(CType, JNIType)
	static private HashMap<String, Pair<String, String>> types = null;
	static private HashMap<String, Pair<String, String>> numericTypes = null;

	private static void initTypes() {
		PrimitiveTypes.types = new HashMap<>();
		PrimitiveTypes.numericTypes = new HashMap<>();
		PrimitiveTypes.numericTypes.put("int", new Pair<String, String>("int",
				"jint"));
		PrimitiveTypes.numericTypes.put("long", new Pair<String, String>(
				"long", "jlong"));
		PrimitiveTypes.numericTypes.put("float", new Pair<String, String>(
				"float", "jfloat"));
		PrimitiveTypes.numericTypes.put("double", new Pair<String, String>(
				"double", "jdouble"));
		PrimitiveTypes.numericTypes.put("short", new Pair<String, String>(
				"short", "jshort"));
		PrimitiveTypes.types.putAll(numericTypes);
		PrimitiveTypes.types.put("char", new Pair<String, String>("char",
				"jchar"));
		PrimitiveTypes.types.put("boolean", new Pair<String, String>("bool",
				"jboolean"));
		PrimitiveTypes.types.put("byte", new Pair<String, String>("byte",
				"jbyte"));

	}

	/**
	 * Checks if a given type is a Java primitive type.
	 * 
	 * @param type
	 *            Type name.
	 * @return True if type is primitive. False otherwise.
	 */
	public static boolean isPrimitive(String type) {
		if (PrimitiveTypes.types == null)
			PrimitiveTypes.initTypes();
		return PrimitiveTypes.types.containsKey(type);
	}

	/**
	 * Checks if a given type is a numeric Java primitive type.
	 * 
	 * @param type
	 *            Type name.
	 * @return True if type is numeric and primitive. False otherwise.
	 */
	public static boolean isNumericPrimitive(String type) {
		if (PrimitiveTypes.numericTypes == null)
			PrimitiveTypes.initTypes();
		return PrimitiveTypes.numericTypes.containsKey(type);
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
		if (PrimitiveTypes.types == null)
			PrimitiveTypes.initTypes();
		if (!PrimitiveTypes.isPrimitive(type))
			return "";
		else
			return PrimitiveTypes.types.get(type).left;
	}

	/**
	 * Gets the equivalent JNI type for a given Java primitive type.
	 * 
	 * @param type
	 *            Java type name.
	 * @return JNI equivalent type. Empty if type informed is not Java primitive
	 *         or is not recognized among the primitives available in this
	 *         class.
	 */
	public static String getJNIType(String type) {
		if (PrimitiveTypes.types == null)
			PrimitiveTypes.initTypes();
		if (!PrimitiveTypes.isPrimitive(type))
			return "";
		else
			return PrimitiveTypes.types.get(type).right;
	}
}
