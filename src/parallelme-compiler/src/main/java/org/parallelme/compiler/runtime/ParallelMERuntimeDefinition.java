/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.parallelme.compiler.RuntimeDefinitionImpl;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.parallelme.compiler.util.FileWriter;
import org.stringtemplate.v4.ST;

/**
 * Definitions for ParallelME runtime.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeDefinition extends RuntimeDefinitionImpl {
	private ParallelMERuntimeJavaFile javaContentCreation = new ParallelMERuntimeJavaFile();
	private ParallelMERuntimeCppHppFile cppHppContentCreation = new ParallelMERuntimeCppHppFile();

	public ParallelMERuntimeDefinition(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		super(cCodeTranslator, outputDestinationFolder);
		this.initTranslators();
	}

	private void initTranslators() {
		if (super.translators == null) {
			super.translators = new HashMap<>();
			super.translators.put(HDRImage.getName(), new PMHDRImageTranslator(
					this.cCodeTranslator));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TargetRuntime getTargetRuntime() {
		return TargetRuntime.ParallelME;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getIsValidBody() {
		ArrayList<String> ret = new ArrayList<>();
		ret.add(this.commonDefinitions.getPrefix() + "runtimePointer != 0;");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getInitializationString(String packageName,
			String className) {
		return new ArrayList<>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImports(List<UserLibraryData> iteratorsAndBinds) {
		StringBuffer ret = new StringBuffer();
		ret.append("import org.parallelme.runtime.ParallelMERuntimeJNIWrapper;\n");
		ret.append(this.getUserLibraryImports(iteratorsAndBinds));
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean translateIteratorsAndBinds(String packageName,
			String className, List<Iterator> iterators,
			List<InputBind> inputBinds, List<OutputBind> outputBinds) {
		this.createJNIFiles(packageName, className, inputBinds, iterators,
				outputBinds);
		this.createKernelFiles(packageName, className, inputBinds, iterators,
				outputBinds);
		return true;
	}

	/**
	 * Create the JNI file that will be used to connect the Java environment
	 * with ParallelME runtime.
	 * 
	 * In order to simplify the source code generation, CPP and H files are
	 * created together in this method. Though method declaration in H files
	 * does not need variable names, they are declared in the generated file in
	 * order to reduce complexity and increase maintainability of compiler code.
	 */
	private void createJNIFiles(String packageName, String className,
			List<InputBind> inputBinds, List<Iterator> iterators,
			List<OutputBind> outputBinds) {
		String jniJavaClassName = this.getJNIWrapperClassName(className);
		FileWriter.writeFile(jniJavaClassName + ".java", this.commonDefinitions
				.getJavaDestinationFolder(this.outputDestinationFolder,
						packageName), this.javaContentCreation
				.getJavaJNIWrapperClass(packageName, jniJavaClassName,
						iterators));
		String jniCClassName = commonDefinitions.getCClassName(packageName,
				jniJavaClassName);
		FileWriter.writeFile(jniCClassName + ".cpp", this.commonDefinitions
				.getJNIDestinationFolder(this.outputDestinationFolder),
				this.cppHppContentCreation.getCppJNIWrapperClass(packageName,
						jniJavaClassName, iterators));
		FileWriter.writeFile(jniCClassName + ".hpp", this.commonDefinitions
				.getJNIDestinationFolder(this.outputDestinationFolder),
				this.cppHppContentCreation.getHppJNIWrapperClass(packageName,
						jniJavaClassName, iterators));
	}

	private String getJNIWrapperClassName(String className) {
		return className + "JNIWrapper";
	}

	/**
	 * Create the kernel file that will be used to store the user code written
	 * in the user library.
	 * 
	 * In order to simplify the source code generation, CPP and H files are
	 * created together in this method. Though method declaration in H files
	 * does not need variable names, they are declared in the generated file in
	 * order to reduce complexity and increase maintainability of compiler code.
	 */
	private void createKernelFiles(String packageName, String className,
			List<InputBind> inputBinds, List<Iterator> iterators,
			List<OutputBind> outputBinds) {
		String templateKernelFile = "<introductoryMsg>\n"
				+ "#ifndef KERNELS_H\n" + "#define KERNELS_H\n\n"
				+ "const char kernels[] =\n"
				+ "\t<kernels:{var|\"<var.line>\"\n}>" + "#endif\n";
		ST st = new ST(templateKernelFile);
		// 1. Add header comment
		st.add("introductoryMsg", this.commonDefinitions.getHeaderComment());
		// 2. Translate input binds
		Set<String> inputBindTypes = new HashSet<String>();
		for (InputBind inputBind : inputBinds) {
			if (!inputBindTypes.contains(inputBind.variable.typeName)) {
				inputBindTypes.add(inputBind.variable.typeName);
				String kernel = this.translators.get(
						inputBind.variable.typeName).translateInputBind(
						className, inputBind);
				this.addKernelByLine(kernel, st);
			}
		}
		// 3. Translate iterators
		for (Iterator iterator : iterators) {
			String kernel = this.translators.get(iterator.variable.typeName)
					.translateIterator(className, iterator);
			this.addKernelByLine(kernel, st);
		}
		// 4. Translate outputbinds
		Set<String> outputBindTypes = new HashSet<String>();
		for (OutputBind outputBind : outputBinds) {
			if (!outputBindTypes.contains(outputBind.variable.typeName)) {
				outputBindTypes.add(outputBind.variable.typeName);
				String kernel = this.translators.get(
						outputBind.variable.typeName).translateOutputBind(
						className, outputBind);
				this.addKernelByLine(kernel, st);
			}
		}
		FileWriter.writeFile("kernels.h", this.commonDefinitions
				.getJNIDestinationFolder(this.outputDestinationFolder), st
				.render());
	}

	/**
	 * Add a given kernel line-by-line to the string template informed.
	 * 
	 * @param kernel
	 *            Multi-line kernel function.
	 * @param st
	 *            String template with "kernes.line" parameter.
	 */
	private void addKernelByLine(String kernel, ST st) {
		String[] lines = kernel.split("\n");
		for (String line : lines) {
			st.addAggr("kernels.{line}", line + "\\n");
		}
		st.addAggr("kernels.{line}", "\\n");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exportInternalLibrary(String packageName,
			String destinationFolder) throws IOException {
		// Copy all files and directories under ParallelME resource folder to
		// the destination folder.
		this.exportResource("ParallelME", destinationFolder);
		this.exportResource("Common", destinationFolder);
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(MethodCall methodCall) {
		String ret = "";
		return ret;
	}
}
