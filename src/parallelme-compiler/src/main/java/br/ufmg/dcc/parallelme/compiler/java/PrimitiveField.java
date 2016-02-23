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
 * 
 * @author Pedro Caldeira
 */
public class PrimitiveField {

	private final String name;
	private final PrimitiveType primitiveType;

	public PrimitiveField(String name, String type) {
		this.name = name;
		this.primitiveType = PrimitiveType.getValueOf(type);
	}

	public String getName() {
		return this.name;
	}

	public PrimitiveType getPrimitiveType() {
		return this.primitiveType;
	}
}
