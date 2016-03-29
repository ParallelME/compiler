/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime.translation.data;

/**
 * Basic information for runtime variable parameters.
 * 
 * @author Wilson de Carvalho
 * @see ParameterDescriptor
 */
public class Variable implements Parameter {
	public final String name;
	public final String typeName;
	public final String typeParameterName;
	public final String modifier;

	public Variable(String name, String typeName, String typeParameter,
			String modifier) {
		this.name = name;
		this.typeName = typeName;
		this.typeParameterName = typeParameter;
		this.modifier = modifier;
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

	@Override
	public int hashCode() {
		return this.name.hashCode() * 3 + this.typeName.hashCode() * 7
				+ this.typeParameterName.hashCode() * 11
				+ this.modifier.hashCode() * 13;
	}
}
