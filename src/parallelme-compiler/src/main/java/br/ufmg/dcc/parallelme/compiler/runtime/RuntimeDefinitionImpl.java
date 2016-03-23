/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
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

import org.apache.commons.io.FileUtils;

import br.ufmg.dcc.parallelme.compiler.SimpleLogger;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;

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

	/**
	 * Return an unique iterator name base on its sequential number.
	 */
	protected String getIteratorName(Iterator iterator) {
		return iteratorName + iterator.sequentialNumber;
	}

	/**
	 * Return an unique input bind name base on its sequential number.
	 */
	protected String getInputBindName(InputBind inputBind) {
		return inputBindName + inputBind.sequentialNumber;
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
}
