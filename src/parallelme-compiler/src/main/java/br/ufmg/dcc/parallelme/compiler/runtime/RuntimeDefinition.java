/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import br.ufmg.dcc.parallelme.compiler.runtime.translationdata.*;

/**
 * All those runtime specific definitions must be implemented on classes derived
 * from this interface.
 * 
 * @author Wilson de Carvalho
 */
public interface RuntimeDefinition {
	/**
	 * Get the initialization string for a given variable definition.
	 * 
	 * @return Initialization string.
	 */
	public String getInitializationString();

	/**
	 * Get the initialization string for a given variable definition.
	 * 
	 * @param firstFunctionNumber
	 *            The number of the first function that will be in initialized.
	 * @param functionCount
	 *            Number of functions that will be created.
	 * 
	 * @return Initialization string.
	 */
	public String getFunctionInitializationString(int firstFunctionNumber,
			int functionCount);

	/**
	 * Get the necessary imports for the runtime.
	 * 
	 * @return String with the necessary imports.
	 */
	public String getImports();

	/**
	 * Create an allocation definition for the informed expression context.
	 * 
	 * @param inputBind
	 *            Object containing the necessary information to build an
	 *            allocation.
	 * 
	 * @return A string with the declaration for the new allocation.
	 */
	public String getCreateAllocationString(InputBind inputBind);

	/**
	 * Creates the code that is necessary to perform input binding.
	 * 
	 * @param inputBind
	 *            Object containing the necessary information to build an
	 *            allocation.
	 * 
	 * @return A string with the declaration for the new allocation.
	 */
	public String getCreateAllocationFunction(InputBind inputBind);

	/**
	 * Gets the data back from the allocation.
	 * 
	 * @param outputBind
	 *            Object containing the necessary information to perform the
	 *            binding from an allocation to a destination object.
	 * 
	 * @return A string with the code to get the data from the allocation.
	 */
	public String getAllocationDataOutputString(OutputBind outputBind);

	/**
	 * Creates the code that is necessary to perform ouput binding.
	 * 
	 * @param outputBind
	 *            Object containing the necessary information to perform the
	 *            binding from an allocation to a destination object.
	 * 
	 * @return A string with the code to get the data from the allocation.
	 */
	public String getAllocationDataOutputFunction(OutputBind outputBind);

	/**
	 * Runs an iterator function.
	 * 
	 * @param userLibraryObject
	 *            Description of the original user library object that will be
	 *            used to create the allocation.
	 * @param functionNumber
	 *            The sequential number representing the function that will be
	 *            created along with the allocation statements.
	 * @return A string with the code to call an iterator function.
	 */
	public String getIteratorString(Variable userLibraryObject,
			int functionNumber);

	/**
	 * Translates user library objects declared on the C code to a correspondent
	 * runtime-specific type. Example: replaces all RGBA objects by float3 on
	 * RenderScript.
	 * 
	 * @param userLibraryObject
	 *            Description of the original user library object that will be
	 *            used to create the allocation.
	 * @param code
	 *            Original code that must have the reference replaced.
	 * @return A string with the new code replaced.
	 */
	public String translateUserLibraryType(Variable userLibraryObject,
			String code);

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
}
