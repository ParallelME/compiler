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
	 * @return Initialization string.
	 */
	public String getInitializationString();

	/**
	 * Get the initialization string for a given variable definition.
	 * 
	 * @param firstFunctionNumber
	 *            The number of the first function that will be initialized.
	 * @param functionCount
	 *            Number of functions that will be created.
	 * 
	 * @return Initialization string.
	 */
	public String getFunctionInitializationString(int firstFunctionNumber,
			int functionCount);

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
	 * @param inputBind
	 *            Object containing the necessary information to build an
	 *            allocation.
	 * 
	 * @return A string with the creation for the new allocation.
	 */
	public String createAllocation(InputBind inputBind);

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
	 * Creates the code that is necessary to perform input binding.
	 * 
	 * @param inputBind
	 *            Object containing the necessary information to build an
	 *            allocation.
	 * 
	 * @return A string with the declaration for the new allocation.
	 */
	public String createAllocationFunction(InputBind inputBind);

	/**
	 * Gets the data back from the allocation.
	 * 
	 * @param outputBind
	 *            Object containing the necessary information to perform the
	 *            binding from an allocation to a destination object.
	 * 
	 * @return A string with the code to get the data from the allocation.
	 */
	public String getAllocationData(OutputBind outputBind);

	/**
	 * Creates the code that is necessary to perform ouput binding.
	 * 
	 * @param outputBind
	 *            Object containing the necessary information to perform the
	 *            binding from an allocation to a destination object.
	 * 
	 * @return A string with the code to get the data from the allocation.
	 */
	public String getAllocationDataFunction(OutputBind outputBind);

	/**
	 * Returns the code that will be placed in the translated Java code to run a
	 * given iterator.
	 * 
	 * @param iterator
	 *            The iterator that must be called from the Java code.
	 *
	 * @return A string with the code to call the iterator.
	 */
	public String getIterator(Iterator iterator);

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
	 * Create a unique name for a given function number.
	 * 
	 * @param functionNumber
	 *            Function number.
	 */
	public String getFunctionName(int functionNumber);

	/**
	 * Translates a given iterator code.
	 * 
	 * @param iterator
	 *            Iterator that must be translated.
	 * 
	 * @return A string with the translated code that can be stored in a file.
	 */
	public String translateIteratorCode(Iterator iterator);

	/**
	 * Returns the file extension that will be used to create the output files
	 * for each C file.
	 */
	public String getCFileExtension();

	/**
	 * Returns a string with a common C file header.
	 * 
	 * @param packageName
	 *            Name of the function package.
	 */
	public String getCFunctionHeader(String packageName);

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
