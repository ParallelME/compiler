/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.symboltable;

import org.antlr.v4.runtime.Token;

/**
 * Token address.
 * 
 * @author Wilson de Carvalho
 */
public class TokenAddress {
	public final Token start, stop;

	public TokenAddress(Token start, Token stop) {
		this.start = start;
		this.stop = stop;
	}

	@Override
	public boolean equals(Object other) {
		if (this.getClass() != other.getClass()) {
			return false;
		} else {
			TokenAddress foo = (TokenAddress) other;
			return foo.start == this.start && foo.stop == this.stop;
		}
	}
}