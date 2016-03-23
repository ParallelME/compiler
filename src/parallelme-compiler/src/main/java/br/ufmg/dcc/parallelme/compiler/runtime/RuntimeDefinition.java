/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import java.io.IOException;
import java.util.List;

import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.InputBind;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Iterator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.MethodCall;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.OutputBind;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.UserLibraryData;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Variable;

/**
 * All those runtime specific definitions must be implemented on classes derived
 * from this interface.
 * 
 * @author Wilson de Carvalho
 */
public interface RuntimeDefinition {
	/**
	 * Get the initialization string for this runtime.
	 * 
	 * @param packageName
	 *            Current package name.
	 * @param className
	 *            Current class name.
	 * @param iterators
	 *            List of all existing iterators on the given class name.
	 * 
	 * @return Initialization string.
	 */
	public String getInitializationString(String packageName, String className,
			List<Iterator> iterators);

	/**
	 * Get the necessary imports for this runtime.
	 * 
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found in a given class.
	 * 
	 * @return String with the necessary imports.
	 */
	public String getImports(List<UserLibraryData> iteratorsAndBinds);

	/**
	 * Create an allocation for the informed input bind.
	 * 
	 * @param className
	 *            Current class name.
	 * @param inputBind
	 *            Object containing the necessary information to build an
	 *            allocation.
	 * 
	 * @return A string with the creation for the new allocation.
	 */
	public String createAllocation(String className, InputBind inputBind);

	/**
	 * Declare an allocation based on the informed expression context.
	 * 
	 * @param inputBind
	 *            Object containing the necessary information to declare an
	 *            allocation.
	 * 
	 * @return A string with the declaration for the new allocation.
	 */
	public String declareAllocation(InputBind inputBind);

	/**
	 * Gets the data back from the allocation.
	 * 
	 * @param className
	 *            Current class name.
	 * @param outputBind
	 *            Object containing the necessary information to perform the
	 *            binding from an allocation to a destination object.
	 * 
	 * @return A string with the code to get the data from the allocation.
	 */
	public String getAllocationData(String className, OutputBind outputBind);

	/**
	 * Returns the code that will be placed in the translated Java code to run a
	 * given iterator.
	 * 
	 * @param className
	 *            Current class name.
	 * @param iterator
	 *            The iterator that must be called from the Java code.
	 *
	 * @return A string with the code to call the iterator.
	 */
	public String getIteratorCall(String className, Iterator iterator);

	/**
	 * Translates a given type to an equivalent runtime type. Example: translate
	 * RGB type to float3 on RenderScript.
	 * 
	 * @param typeName
	 *            Type that must be translated.
	 * @return A string with the equivalent type for this runtime.
	 */
	public String translateType(String typeName);

	/**
	 * Translates variables on the give code to a correspondent runtime-specific
	 * type. Example: replaces all RGB objects by float3 on RenderScript.
	 * 
	 * @param variable
	 *            Variable that must be translated.
	 * @param code
	 *            Original code that must have the reference replaced.
	 * @return A string with the new code with the variable replaced.
	 */
	public String translateVariable(Variable variable, String code);

	/**
	 * Translates a given method call to a runtime-variable equivalent
	 * operation.
	 * 
	 * @param methodCall
	 *            Method call that must be translated
	 * @return A string with the new code for method call replacement.
	 */
	public String translateMethodCall(MethodCall methodCall);

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
			List<InputBind> inputBinds, List<OutputBind> outputBinds);

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
}
