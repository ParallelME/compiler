/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

/**
 * Intermediate representation for code expression.
 * 
 * @author Wilson de Carvalho
 */
public class Expression implements Parameter {
	public final String text;

	public Expression(String text) {
		this.text = text;
	}

	@Override
	public boolean equals(Object other) {
		if (other.getClass() == this.getClass()) {
			return ((Expression) other).text == this.text;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return this.text;
	}

	@Override
	public int hashCode() {
		return this.text.hashCode();
	}
}
