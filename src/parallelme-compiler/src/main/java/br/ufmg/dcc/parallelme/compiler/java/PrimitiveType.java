/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.java;

/**
 * Java primitive types definitions.
 * 
 * @author Pedro Caldeira
 */
public enum PrimitiveType {
	BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE;

	public static PrimitiveType getValueOf(String s) {
		switch (s) {
		case "boolean":
			return BOOLEAN;
		case "char":
			return CHAR;
		case "byte":
			return BYTE;
		case "short":
			return SHORT;
		case "int":
			return INT;
		case "long":
			return LONG;
		case "float":
			return FLOAT;
		case "double":
			return DOUBLE;
		default:
			return null;
		}
	}
}
