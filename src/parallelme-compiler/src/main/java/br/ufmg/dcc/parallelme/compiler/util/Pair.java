/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.util;

/**
 * Class created to store tuples.
 * 
 * It was created to avoid adding Apache Commons <b>solely</b> because of a Pair
 * class. In case this dependency is added to the project for some another
 * reason, please replace this class by org.apache.commons.lang3.tuple.Pair
 * class.
 * 
 * @author Wilson de Carvalho
 */
public class Pair<L, R> {
	public final L left;
	public final R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public boolean equals(Object other) {
		if (this.getClass() == other.getClass()) {
			return ((Pair<?, ?>) other).left == this.left
					&& ((Pair<?, ?>) other).right == this.right;
		} else {
			return false;
		}
	}
}
