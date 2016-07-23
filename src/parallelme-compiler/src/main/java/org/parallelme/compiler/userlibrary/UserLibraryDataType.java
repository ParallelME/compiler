/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary;

/**
 * Definitions for user library data type classes.
 * 
 * @author Wilson de Carvalho
 */
public abstract class UserLibraryDataType extends UserLibraryClass {
	/**
	 * Gets RenderScript Java equivalent type.
	 */
	abstract public String getRenderScriptJavaType();

	/**
	 * Gets RenderScript C equivalent type.
	 */
	abstract public String getRenderScriptCType();
}
