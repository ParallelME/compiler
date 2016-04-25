/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime;

import java.io.IOException;
import java.util.List;

import org.stringtemplate.v4.ST;

import br.ufmg.dcc.parallelme.compiler.runtime.translation.CTranslator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClass;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.BitmapImage;
import br.ufmg.dcc.parallelme.compiler.userlibrary.classes.HDRImage;
import br.ufmg.dcc.parallelme.compiler.util.FileWriter;

/**
 * Definitions for ParallelME runtime.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeDefinition extends RuntimeDefinitionImpl {
	private static final String templateCreateAllocationJavaHDRImage = "RGBE.ResourceData <resourceData> = RGBE.loadFromResource(<params>);\n"
			+ "\t<varName>Worksize = <resourceData>.width * <resourceData>.height;\n"
			+ "\t<varName>ResourceDataId = <jniJavaClassName>.getInstance().getNewResourceId();";
	private static final String templateCallJNIFunction = "<jniJavaClassName>.getInstance().<functionName>(<resourceDataId><params:{var|, <var.name>}>)";
	private static final String templateCreateAllocation = "int <varName>Worksize, <varName>ResourceDataId;\n";

	private ParallelMERuntimeJavaFile javaContentCreation = new ParallelMERuntimeJavaFile();
	private ParallelMERuntimeCppHppFile cppHppContentCreation = new ParallelMERuntimeCppHppFile();

	public ParallelMERuntimeDefinition(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		super(cCodeTranslator, outputDestinationFolder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getInitializationString(String packageName, String className,
			List<InputBind> inputBinds, List<Iterator> iterators,
			List<OutputBind> outputBinds) {
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
	public String createAllocation(String className, InputBind inputBind) {
		String ret = "";
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.create(inputBind.getVariable().typeName);
		if (userLibraryClass instanceof BitmapImage) {

		} else if (userLibraryClass instanceof HDRImage) {
			String resourceData = this.commonDefinitions.getPrefix()
					+ inputBind.getVariable() + "ResourceData";
			ST st = new ST(templateCreateAllocationJavaHDRImage);
			st.add("resourceData", resourceData);
			st.add("params", this.commonDefinitions
					.toCommaSeparatedString(inputBind.getParameters()));
			st.add("varName",
					this.commonDefinitions.getPrefix()
							+ inputBind.getVariable().name);
			st.add("jniJavaClassName", this.getJNIWrapperClassName(className));
			ret = st.render();
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String declareAllocation(InputBind inputBind) {
		StringBuilder ret = new StringBuilder();
		ST st = new ST(templateCreateAllocation);
		st.add("varName",
				this.commonDefinitions.getPrefix()
						+ inputBind.getVariable().name);
		ret.append(st.render());
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAllocationData(String className, OutputBind outputBind) {
		StringBuilder ret = new StringBuilder();
		Variable variable = outputBind.getVariable();
		String jniJavaClassName = this.getJNIWrapperClassName(className);
		if (variable.typeName.equals(BitmapImage.getName())
				|| variable.typeName.equals(HDRImage.getName())) {
			ST st = new ST(templateCallJNIFunction);
			st.add("jniJavaClassName", jniJavaClassName);
			st.add("functionName", "toBitmap");
			st.add("resourceDataId", this.getResourceDataName(variable.name));
			String workSize = this.getWorksizeName(variable.name);
			st.addAggr("params.{name}", workSize);
			st.addAggr("params.{name}", this.getResourceDataName(variable.name));
			// 4 slots. [Red][Green][Blue][Alpha]
			st.addAggr("params.{name}", workSize + " * 4");
			st.addAggr("params.{name}", outputBind.destinationObject.name);
			// sizeof(float) = 4 * (4 slots)
			st.addAggr("params.{name}", workSize + " * 16");
			ret.append(st.render());
		}
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIteratorCall(String className, Iterator iterator) {
		String jniJavaClassName = this.getJNIWrapperClassName(className);
		ST st = new ST(templateCallJNIFunction);
		st.add("jniJavaClassName", jniJavaClassName);
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.add("resourceDataId",
				this.getResourceDataIdName(iterator.getVariable().name));
		st.addAggr("params.{name}",
				this.getWorksizeName(iterator.getVariable().name));
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{name}", variable.name);
		}
		return st.render() + ";";
	}

	private String getResourceDataIdName(String variableName) {
		return this.commonDefinitions.getPrefix() + variableName
				+ "ResourceDataId";
	}

	private String getResourceDataName(String variableName) {
		return this.commonDefinitions.getPrefix() + variableName
				+ "ResourceData";
	}

	private String getWorksizeName(String variableName) {
		return this.commonDefinitions.getPrefix() + variableName + "Worksize";
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
