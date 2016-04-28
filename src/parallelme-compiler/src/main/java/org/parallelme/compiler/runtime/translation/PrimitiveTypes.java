/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime.translation;

import java.util.HashMap;
import java.util.HashSet;

import org.parallelme.compiler.util.Pair;

/**
 * Hold basic primitive data type information that can be used to recognize Java
 * primitive types and translate them to equivalent C or JNI types.
 * 
 * @author Wilson de Carvalho
 */
public class PrimitiveTypes {
	static private HashMap<String, String> cTypes = null;
	static private HashMap<String, String> jniTypes = null;
	static private HashMap<String, Pair<String, String>> runtimeTypes = null;
	static private HashSet<String> numericTypes = null;

	private static void initTypes() {
		cTypes = new HashMap<>();
		jniTypes = new HashMap<>();
		runtimeTypes = new HashMap<>();
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

		jniTypes.put("int", "jint");
		jniTypes.put("float", "jfloat");
		jniTypes.put("short", "jshort");
		jniTypes.put("char", "jchar");
		jniTypes.put("boolean", "jboolean");
		jniTypes.put("byte", "jbyte");

		runtimeTypes.put("int", new Pair<String, String>("INT", "i"));
		runtimeTypes.put("float", new Pair<String, String>("FLOAT", "f"));
		runtimeTypes.put("short", new Pair<String, String>("SHORT", "s"));
		runtimeTypes.put("char", new Pair<String, String>("CHAR", "c"));
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
	 * Checks if a given type is a numeric Java primitive type.
	 * 
	 * @param type
	 *            Type name.
	 * @return True if type is numeric and primitive. False otherwise.
	 */
	public static boolean isNumericPrimitive(String type) {
		if (numericTypes == null)
			initTypes();
		return numericTypes.contains(type);
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
		if (jniTypes == null)
			initTypes();
		if (!isPrimitive(type))
			return "";
		else
			return jniTypes.get(type);
	}

	/**
	 * Gets the equivalent runtime argument type for a given Java primitive
	 * type.
	 * 
	 * @param type
	 *            Java type name.
	 * @return Runtime argument equivalent type. Empty if type informed is not
	 *         Java primitive or is not recognized among the primitives
	 *         available in this class.
	 */
	public static String getRuntimeArgType(String type) {
		if (runtimeTypes == null)
			initTypes();
		if (!isPrimitive(type))
			return "";
		else
			return runtimeTypes.get(type).left;
	}

	/**
	 * Gets the runtime alias for a given Java primitive type.
	 * 
	 * @param type
	 *            Java type name.
	 * @return Alias for the informed type. Empty if type informed is not Java
	 *         primitive or is not recognized among the primitives available in
	 *         this class.
	 */
	public static String getRuntimeAlias(String type) {
		if (runtimeTypes == null)
			initTypes();
		if (!isPrimitive(type))
			return "";
		else
			return runtimeTypes.get(type).right;
	}
}
