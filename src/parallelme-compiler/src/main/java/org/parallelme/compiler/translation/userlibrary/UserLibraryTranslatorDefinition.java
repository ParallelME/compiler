/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.userlibrary;

import java.util.List;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.MethodCall;
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
	 * Creates the java code to create the input bind objects (allocations).
	 * Input bind creation and declaration are separated because the declaration
	 * must be placed on the user code at the same level of user library objects
	 * were declared. Differently, input bind creation must be placed in the
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
	public String translateObjDeclaration(InputBind inputBind);

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
	public String translateObjDeclaration(Operation operation);

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
	 * Translates a given operation returning its C functions compatible for a
	 * given runtime.
	 * 
	 * @param operation
	 *            Operation information.
	 * 
	 * @return A list where each string represents a function with the C code
	 *         for the given runtime.
	 */
	public List<String> translateOperation(Operation operation);

	/**
	 * Translates a given operation returning the Java code that will be used to
	 * call a given runtime code.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param operation
	 *            Operation information.
	 * 
	 * @return A string with the java code for replacement on the original user
	 *         code written with the user library.
	 */
	public String translateOperationCall(String className, Operation operation);

	/**
	 * Translates a given method call returning the Java code that will be used
	 * to call a given runtime code.
	 * 
	 * @param className
	 *            Name of the java class (user class) where the input bind was
	 *            originally created.
	 * @param methodCall
	 *            Method call information.
	 * 
	 * @return A string with the java code for replacement on the original user
	 *         code written with the user library.
	 */
	public String translateMethodCall(String className, MethodCall methodCall);

	/**
	 * Return imports that are necessary to create Java wrapper interfaces.
	 * 
	 * @return A list where each element is a import statement.
	 */
	public List<String> getJavaInterfaceImports();

	/**
	 * Return imports that are necessary to create Java wrapper implementation.
	 * 
	 * @return A list where each element is a import statement.
	 */
	public List<String> getJavaClassImports();
}
