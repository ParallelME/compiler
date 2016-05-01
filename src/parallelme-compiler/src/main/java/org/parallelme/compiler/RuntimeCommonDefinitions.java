/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.io.File;

import org.parallelme.compiler.intermediate.*;

/**
 * Code useful for common runtime definitions.
 * 
 * @author Wilson de Carvalho
 */
public class RuntimeCommonDefinitions {
	private final String inSuffix = "In";
	private final String outSuffix = "Out";
	private final String iteratorName = "iterator";
	private final String inputBindName = "inputBind";
	private final String outputBindName = "outputBind";
	private final String prefix = "$";
	private final String headerComment = "/**                                               _    __ ____\n"
			+ " *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/\n"
			+ " *  |  _ \\/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__\n"
			+ " *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__\n"
			+ " *  |_| /_/ |_|_|\\_\\/_/ |_/____/___/___/____/ /_/  /_/____/\n"
			+ " *\n"
			+ " * Code created automatically by ParallelME compiler.\n"
			+ " */\n";

	public RuntimeCommonDefinitions() {
	}

	public String getVariableInName(Variable variable) {
		return prefix + variable.name + inSuffix;
	}

	public String getVariableOutName(Variable variable) {
		return prefix + variable.name + outSuffix;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getHeaderComment() {
		return this.headerComment;
	}

	/**
	 * Return an unique prefixed iterator name base on its sequential number.
	 */
	public String getPrefixedIteratorName(Iterator iterator) {
		return this.prefix + this.getIteratorName(iterator);
	}

	/**
	 * Return an unique iterator name base on its sequential number.
	 */
	public String getIteratorName(Iterator iterator) {
		return iteratorName + iterator.sequentialNumber;
	}

	/**
	 * Return an unique input bind name base on its sequential number.
	 */
	public String getPrefixedInputBindName(InputBind inputBind) {
		return this.prefix + this.getInputBindName(inputBind);
	}

	/**
	 * Return an unique input bind name base on its sequential number.
	 */
	public String getInputBindName(InputBind inputBind) {
		return inputBindName + inputBind.sequentialNumber;
	}

	/**
	 * Return an unique prefixed output bind name base on its sequential number.
	 */
	public String getPrefixedOutputBindName(OutputBind outputBind) {
		return this.prefix + this.getOutputBindName(outputBind);
	}

	/**
	 * Return an unique output bind name base on its sequential number.
	 */
	public String getOutputBindName(OutputBind outputBind) {
		return outputBindName + outputBind.sequentialNumber;
	}

	/**
	 * Return kernel that must be used in kernel object declarations.
	 */
	public String getKernelName(String className) {
		return this.prefix + "kernel_" + className;
	}

	/**
	 * Transforms an array of parameters in a comma separated string.
	 * 
	 * @param parameters
	 *            Array of parameters.
	 * @return Comma separated string with parameters.
	 */
	public String toCommaSeparatedString(Parameter[] parameters) {
		StringBuilder params = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			params.append(parameters[i]);
			if (i != (parameters.length - 1))
				params.append(", ");
		}
		return params.toString();
	}

	/**
	 * Gets a pointer name for a given variable.
	 */
	public String getPointerName(Variable variable) {
		return this.getPrefix() + variable.name + "Ptr";
	}

	/**
	 * Return a standard C class name based on original Java package and class
	 * names.
	 */
	public String getCClassName(String packageName, String className) {
		return packageName.replaceAll("\\.", "_") + "_" + className;
	}

	/**
	 * Return a destination folder for output files in Java.
	 */
	public String getJavaDestinationFolder(String baseDestinationFolder,
			String packageName) {
		return baseDestinationFolder + File.separator + "java" + File.separator
				+ packageName.replaceAll("\\.", "/") + File.separator;
	}

	/**
	 * Return a destination folder for output JNI files.
	 */
	public String getJNIDestinationFolder(String baseDestinationFolder) {
		return baseDestinationFolder + File.separator + "jni" + File.separator;
	}

	/**
	 * Return a destination folder for output RenderScript files.
	 */
	public String getRSDestinationFolder(String baseDestinationFolder,
			String packageName) {
		return baseDestinationFolder + File.separator
				+ packageName.replaceAll("\\.", "/") + File.separator + "rs"
				+ File.separator;
	}
}
