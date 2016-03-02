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

/**
 * Hold basic primitive data type information that can be used to recognize Java
 * primitive types and translate them to equivalent C types.
 * 
 * @author Wilson de Carvalho
 */
public class PrimitiveTypes {
	static private HashMap<String, String> javaAndCTypes = null;

	private static void initTypes() {
		PrimitiveTypes.javaAndCTypes = new HashMap<>();
		PrimitiveTypes.javaAndCTypes.put("int", "int");
		PrimitiveTypes.javaAndCTypes.put("long", "long");
		PrimitiveTypes.javaAndCTypes.put("float", "float");
		PrimitiveTypes.javaAndCTypes.put("double", "double");
		PrimitiveTypes.javaAndCTypes.put("char", "char");
		PrimitiveTypes.javaAndCTypes.put("boolean", "bool");
	}

	/**
	 * Checks if a given type is a Java primitive type.
	 * 
	 * @param type
	 *            Type name.
	 * @return True if type is primitive. False otherwise.
	 */
	public static boolean isPrimitive(String type) {
		if (PrimitiveTypes.javaAndCTypes == null)
			PrimitiveTypes.initTypes();
		return PrimitiveTypes.javaAndCTypes.containsKey(type);
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
		if (PrimitiveTypes.javaAndCTypes == null)
			PrimitiveTypes.initTypes();
		if (!PrimitiveTypes.isPrimitive(type))
			return "";
		else
			return PrimitiveTypes.javaAndCTypes.get(type);
	}
}
