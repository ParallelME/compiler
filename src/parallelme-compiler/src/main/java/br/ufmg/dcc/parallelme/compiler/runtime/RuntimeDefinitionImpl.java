/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

import br.ufmg.dcc.parallelme.compiler.SimpleLogger;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.HDRImage;

/**
 * Code useful for specfic runtime definition implementation.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RuntimeDefinitionImpl implements RuntimeDefinition {
	private final String inSuffix = "In";
	private final String outSuffix = "Out";
	private final String iteratorName = "iterator";
	private final String inputBindName = "inputBind";
	private final String outputBindName = "outputBind";
	private final String prefix = "$";
	private final String headerComment = "/**                                               _    __ ____\n"
			+ " *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/\n"
			+ " *  |  _ \\/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__\n"
			+ " *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__\n"
			+ " *  |_| /_/ |_|_|\\_\\/_/ |_/____/___/___/____/ /_/  /_/____/\n"
			+ " *\n"
			+ " *  DCC-UFMG\n"
			+ " *\n"
			+ " * Code created automatically by ParallelME compiler.\n"
			+ " */\n";

	protected final CTranslator cCodeTranslator;
	protected final String outputDestinationFolder;

	public RuntimeDefinitionImpl(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		this.cCodeTranslator = cCodeTranslator;
		this.outputDestinationFolder = outputDestinationFolder;
	}

	protected String getVariableInName(Variable variable) {
		return prefix + variable.name + inSuffix;
	}

	protected String getVariableOutName(Variable variable) {
		return prefix + variable.name + outSuffix;
	}

	protected String getPrefix() {
		return prefix;
	}

	protected String getHeaderComment() {
		return this.headerComment;
	}

	/**
	 * Return an unique prefixed iterator name base on its sequential number.
	 */
	protected String getPrefixedIteratorName(Iterator iterator) {
		return this.prefix + this.getIteratorName(iterator);
	}

	/**
	 * Return an unique iterator name base on its sequential number.
	 */
	protected String getIteratorName(Iterator iterator) {
		return iteratorName + iterator.sequentialNumber;
	}

	/**
	 * Return an unique input bind name base on its sequential number.
	 */
	protected String getPrefixedInputBindName(InputBind inputBind) {
		return this.prefix + this.getInputBindName(inputBind);
	}

	/**
	 * Return an unique input bind name base on its sequential number.
	 */
	protected String getInputBindName(InputBind inputBind) {
		return inputBindName + inputBind.sequentialNumber;
	}

	/**
	 * Return an unique prefixed output bind name base on its sequential number.
	 */
	protected String getPrefixedOutputBindName(OutputBind outputBind) {
		return this.prefix + this.getOutputBindName(outputBind);
	}

	/**
	 * Return an unique output bind name base on its sequential number.
	 */
	protected String getOutputBindName(OutputBind outputBind) {
		return outputBindName + outputBind.sequentialNumber;
	}

	/**
	 * Return kernel that must be used in kernel object declarations.
	 */
	protected String getKernelName(String className) {
		return this.prefix + "kernel_" + className;
	}

	/**
	 * Exports a given resource folder and all its contents.
	 * 
	 * @throws IOException
	 */
	protected void exportResource(String resourceName, String destinationFolder)
			throws IOException {
		URL resource = ClassLoader.getSystemClassLoader().getResource(
				resourceName);
		if (resource == null) {
			String msg = resourceName
					+ " resource folder is missing in this JAR. Please recompile the project.";
			SimpleLogger.error(msg);
			throw new RuntimeException(msg);
		}
		File resourceDir = null;
		try {
			resourceDir = new File(resource.toURI());
		} catch (URISyntaxException e) {
			SimpleLogger
					.error(resource
							+ " does not appear to be a valid URL / URI, thus it won't be copied to '"
							+ destinationFolder + "'.");
			resourceDir = null;
		}
		if (resourceDir != null && resourceDir.exists()) {
			// Get the list of the files contained in the package
			String[] list = resourceDir.list();
			for (int i = 0; i < list.length; i++) {
				String fileOrDirName = list[i];
				File source = new File(resourceDir.getAbsolutePath()
						+ File.separator + fileOrDirName);
				File destiny = new File(destinationFolder + File.separator
						+ fileOrDirName);
				if (source.isDirectory()) {
					FileUtils.copyDirectory(source, destiny);
				} else if (source.isFile()) {
					FileUtils.copyFile(source, destiny);
				}
			}
		}
	}

	/**
	 * Transforms an array of parameters in a comma separated string.
	 * 
	 * @param parameters
	 *            Array of parameters.
	 * @return Comma separated string with parameters.
	 */
	protected String toCommaSeparatedString(Parameter[] parameters) {
		StringBuilder params = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			params.append(parameters[i]);
			if (i != (parameters.length - 1))
				params.append(", ");
		}
		return params.toString();
	}

	/**
	 * Return the list of necessary imports for user library classes.
	 * 
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found in a given class.
	 * 
	 * @return String with the necessary imports.
	 */
	protected String getUserLibraryImports(
			List<UserLibraryData> iteratorsAndBinds) {
		StringBuffer imports = new StringBuffer();
		boolean exportedHDR = false;
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (!exportedHDR
					&& userLibraryData.getVariable().typeName.equals(HDRImage
							.getName())) {
				imports.append("import br.ufmg.dcc.parallelme.userlibrary.RGBE;\n");
				exportedHDR = true;
			}
		}
		return imports.toString();
	}
}
