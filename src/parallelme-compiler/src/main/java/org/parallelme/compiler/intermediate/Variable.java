/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

/**
 * Intermediate representation for variable parameters.
 * 
 * @author Wilson de Carvalho
 */
public class Variable implements Parameter {
	public final String name;
	public final String typeName;
	public final String typeParameterName;
	public final String modifier;
	// The sequential number is used to uniquely identify a variable in a class.
	// This number will be used to handle variables inside the runtime wrapper
	// class, avoiding possible name colisions that may be created in different
	// scopes in the original user code.
	public final int sequentialNumber;

	public Variable(String name, String typeName, String typeParameter,
			String modifier, int sequentialNumber) {
		this.name = name;
		this.typeName = typeName;
		this.typeParameterName = typeParameter;
		this.modifier = modifier;
		this.sequentialNumber = sequentialNumber;
	}

	/**
	 * Return true if this variable was declared as final.
	 */
	public boolean isFinal() {
		return modifier.equals("final");
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass()) {
			return ((Variable) other).name == this.name
					&& ((Variable) other).typeName == this.typeName
					&& ((Variable) other).typeParameterName == this.typeParameterName;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return this.name;
	}
}
