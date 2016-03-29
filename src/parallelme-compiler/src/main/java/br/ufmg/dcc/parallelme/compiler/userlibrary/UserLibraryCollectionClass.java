/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.userlibrary;

/**
 * Describes a User Library collection class.
 * 
 * This interface and derived classes were solely created to accomplish a first
 * viable version (prototype) of our compiler. Thus in future version we should
 * eliminate explicit user library classes' declaration and move to a
 * parser-based approach in which the compiler would also analyze user library
 * classes in order to evaluate parameters, methods, constructors and so on.
 * 
 * @author Wilson de Carvalho
 */
public interface UserLibraryCollectionClass extends UserLibraryClass {
	/**
	 * Gets iterator method name.
	 * 
	 * @return Method name.
	 */
	public String getIteratorMethodName();

	/**
	 * Gets the data output method name.
	 * 
	 * @return Method name.
	 */
	public String getDataOutputMethodName();
}
