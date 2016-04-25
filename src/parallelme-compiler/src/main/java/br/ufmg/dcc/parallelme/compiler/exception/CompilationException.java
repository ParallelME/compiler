/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.exception;

public class CompilationException extends Exception {
	private static final long serialVersionUID = -2869143025040591096L;
	private final int lineNumber;

	public CompilationException(String msg) {
		super(msg);
		this.lineNumber = -1;
	}
	
	public CompilationException(String msg, int lineNumber) {
		super(msg);
		this.lineNumber = lineNumber;
	}

	public int getLineNumber() {
		return lineNumber;
	}
}
