/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.userlibrary;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.OutputBind;

/**
 * Interface with all definitions for user library classes' translation.
 * 
 * @author Wilson de Carvalho
 */
public interface UserLibraryTranslatorDefinition {
	/**
	 * Translates a given input bind returning the C code compatible for a given
	 * runtime.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param inputBind
	 *            Input bind information.
	 * 
	 * @return A string with the C code for the given runtime.
	 */
	public String translateInputBind(String className, InputBind inputBind);

	/**
	 * Translates a given input bind returning the Java code that will be used
	 * to call a given runtime code.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param inputBind
	 *            Input bind information.
	 * 
	 * @return A string with the java code for replacement on the original user
	 *         code written with the user library.
	 */
	public String translateInputBindCall(String className, InputBind inputBind);

	/**
	 * Creates the java code to create the input bind objects (allocations).
	 * Input bind creation and declaration are separated because the declaration
	 * must be placed on the user code at the same level of user library objects
	 * were declared. Differently, input bind creation must be placed on the
	 * same level where user library objects are instantiated.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param inputBind
	 *            Input bind information.
	 *
	 * @return
	 */
	public String translateInputBindObjCreation(String className,
			InputBind inputBind);

	/**
	 * Creates the Java code declaration for a given input bind. Input bind
	 * creation and declaration are separated because the declaration must be
	 * placed on the user code at the same level of user library objects were
	 * declared. Differently, input bind creation must be placed on the same
	 * level where user library objects are instantiated.
	 * 
	 * @param inputBind
	 *            Input bind information.
	 *
	 * @return
	 */
	public String translateInputBindObjDeclaration(InputBind inputBind);

	/**
	 * Translates a given output bind returning the C code compatible for a
	 * given runtime.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param outputBind
	 *            Output bind information.
	 * 
	 * @return A string with the C code for the given runtime.
	 */
	public String translateOutputBind(String className, OutputBind outputBind);

	/**
	 * Translates a given output bind returning the Java code that will be used
	 * to call a given runtime code.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param outputBind
	 *            Output bind information.
	 * 
	 * @return A string with the java code for replacement on the original user
	 *         code written with the user library.
	 */
	public String translateOutputBindCall(String className,
			OutputBind outputBind);

	/**
	 * Translates a given iterator returning the C code compatible for a given
	 * runtime.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param iterator
	 *            Iterator information.
	 * 
	 * @return A string with the C code for the given runtime.
	 */
	public String translateIterator(String className, Iterator iterator);

	/**
	 * Translates a given iterator returning the Java code that will be used to
	 * call a given runtime code.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param iterator
	 *            Iterator information.
	 * 
	 * @return A string with the java code for replacement on the original user
	 *         code written with the user library.
	 */
	public String translateIteratorCall(String className, Iterator iterator);
}
