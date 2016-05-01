/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.parallelme.compiler.RuntimeDefinitionImpl;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.parallelme.compiler.util.FileWriter;

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
			super.translators.put(HDRImage.getName(),
					new PMHDRImageTranslator());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInitializationString(String packageName, String className) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImports(List<UserLibraryData> iteratorsAndBinds) {
		StringBuffer ret = new StringBuffer();
		ret.append(this.getUserLibraryImports(iteratorsAndBinds));
		ret.append("\n");
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
		FileWriter.writeFile("kernel.h", this.commonDefinitions
				.getJNIDestinationFolder(this.outputDestinationFolder),
				this.cppHppContentCreation.getHKernelFile(packageName,
						className, inputBinds, iterators, outputBinds));
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
