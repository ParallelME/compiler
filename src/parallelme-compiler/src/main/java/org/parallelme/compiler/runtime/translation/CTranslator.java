/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime.translation;

/**
 * Defines the contract of a C code translator that will be used to translate
 * iterators' code to C.
 * 
 * @author Wilson de Carvalho
 */
public interface CTranslator {
	/**
	 * Translate an informed Java code to C.
	 * 
	 * @param javaCode
	 *            Java code that must be translated to C.
	 * @return String with code translated to C.
	 */
	public String translate(String javaCode);
}
