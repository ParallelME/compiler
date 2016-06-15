/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.util.List;
import java.util.Map;

import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;
import org.parallelme.compiler.userlibrary.classes.HDRImage;

/**
 * Code useful for specfic runtime definition implementation.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RuntimeDefinitionImpl implements RuntimeDefinition {
	protected final CTranslator cCodeTranslator;
	protected final String outputDestinationFolder;
	// Keeps a key-value map of all user library translators that will be used
	// to translate this runtime.
	protected Map<String, UserLibraryTranslatorDefinition> translators = null;

	public RuntimeDefinitionImpl(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		this.cCodeTranslator = cCodeTranslator;
		this.outputDestinationFolder = outputDestinationFolder;
	}

	/**
	 * Return the list of necessary imports for user library classes.
	 * 
	 * @param operationsAndBinds
	 *            List of all operations and binds found in a given class.
	 * 
	 * @return String with the necessary imports.
	 */
	protected String getUserLibraryImports(
			List<UserLibraryData> operationsAndBinds) {
		StringBuffer imports = new StringBuffer();
		boolean exportedHDR = false;
		for (UserLibraryData userLibraryData : operationsAndBinds) {
			if (!exportedHDR
					&& userLibraryData.variable.typeName.equals(HDRImage
							.getInstance().getClassName())) {
				imports.append("import org.parallelme.userlibrary.RGBE;\n");
				exportedHDR = true;
			}
		}
		return imports.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UserLibraryTranslatorDefinition getTranslator(String typeName)
			throws CompilationException {
		if (!translators.containsKey(typeName))
			throw new CompilationException(typeName
					+ " is an invalid type for runtime compilation.");
		return translators.get(typeName);
	}
}
