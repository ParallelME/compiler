/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.io.IOException;
import java.util.List;

import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.UserLibraryData;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;

/**
 * All those runtime specific definitions must be implemented on classes derived
 * from this interface.
 * 
 * @author Wilson de Carvalho
 */
public interface RuntimeDefinition {
	public enum TargetRuntime {
		RenderScript, ParallelME;
	}

	/**
	 * Get the initialization string for this runtime.
	 * 
	 * @param packageName
	 *            Current package name.
	 * @param className
	 *            Current class name.
	 * 
	 * @return Initialization string.
	 */
	public String getInitializationString(String packageName, String className)
			throws CompilationException;

	/**
	 * Get the necessary imports for this runtime.
	 * 
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found in a given class.
	 * 
	 * @return String with the necessary imports.
	 */
	public String getImports(List<UserLibraryData> iteratorsAndBinds)
			throws CompilationException;

	/**
	 * Translates a given method call to a runtime-variable equivalent
	 * operation.
	 * 
	 * @param methodCall
	 *            Method call that must be translated
	 * @return A string with the new code for method call replacement.
	 */
	public String translateMethodCall(MethodCall methodCall)
			throws CompilationException;

	/**
	 * Translates a list of iterators.
	 * 
	 * @param packageName
	 *            Name of the package of which current data (class, iterators
	 *            and binds) belong.
	 * @param className
	 *            Name of the class of which current data (iterators and binds)
	 *            belong.
	 * @param iterators
	 *            Iterator that must be translated.
	 * @param inputBinds
	 *            Input binds that must be translated.
	 * @param outputBinds
	 *            Output binds that must be translated.
	 * 
	 * @return True if the translation was successful. False otherwise.
	 */
	public boolean translateIteratorsAndBinds(String packageName,
			String className, List<Iterator> iterators,
			List<InputBind> inputBinds, List<OutputBind> outputBinds)
			throws CompilationException;

	/**
	 * Exports the internal library files needed for this runtime.
	 * 
	 * @param packageName
	 *            Package name that will be used for files that will be
	 *            exported.
	 * @param destinationFolder
	 *            Destination folder.
	 * 
	 * @throws IOException
	 *             Exception thrown in case of failures during file or directory
	 *             handling.
	 */
	public void exportInternalLibrary(String packageName,
			String destinationFolder) throws IOException;

	/**
	 * Returns a translator for a given class type name.
	 * 
	 * @param typeName
	 *            String that refers to a valid user library class name.
	 * @return A translator for the specified class type name for the current
	 *         target runtime.
	 * @throws CompilationException
	 *             Throw whenever a non-supported class type is informed.
	 */
	public UserLibraryTranslatorDefinition getTranslator(String typeName)
			throws CompilationException;
}
