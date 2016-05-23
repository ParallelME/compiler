/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.IteratorsAndBinds;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.parallelme.compiler.util.FileWriter;
import org.stringtemplate.v4.ST;

/**
 * Class responsible for all C-related translations.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeCTranslation {
	private static final String templateCPPFile = "<introductoryMsg>\n"
			+ "#include <cClassName>.h\n\n" + "#include \\<memory>\n"
			+ "#include \\<stdexcept>\n" + "#include \\<android/log.h>\n"
			+ "#include \\<parallelme/ParallelME.hpp>\n"
			+ "#include \\<parallelme/SchedulerHEFT.hpp>\n\n"
			+ "using namespace parallelme;\n\n"
			+ "<iterator:{var|<var.body>}; separator=\"\n\n\">";
	private static final String templateIteratorFunctionDecl = "JNIEXPORT void JNICALL Java_<cClassName>_<iteratorName>(JNIEnv *env, jobject self, jlong rtmPtr, jlong varPtr<params:{var|, <var.type> <var.name>}>)";
	private static final String templateParallelIterator = "<functionDecl> {\n"
			+ "\tauto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
			+ "\tauto variablePtr = (<objectType> *) varPtr;\n"
			+ "\tauto task = std::make_unique\\<Task>(runtimePtr->program);\n"
			+ "\ttask->addKernel(\"<iteratorName>\");\n"
			+ "\ttask->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\tkernelHash[\"<iteratorName>\"]\n"
			+ "\t\t\t->setArg(0, variablePtr->outputBuffer)\n"
			+ "\t\t\t->setWorkSize(variablePtr->workSize);\n" + "\t\\});\n"
			+ "\truntimePtr->runtime->submitTask(std::move(task));\n"
			+ "\truntimePtr->runtime->finish();\n" + "\\}";
	private static final String templateSequentialIterator = "<functionDecl> {\n"
			+ "\tauto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
			+ "\tauto variablePtr = (<objectType> *) varPtr;\n"
			+ "\t<buffers:{var|auto <var.name>Buffer = std::make_shared\\<Buffer>(sizeof(<var.name>);\n}>"
			+ "\tauto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f, 2.0f));\n"
			+ "\ttask->addKernel(\"<iteratorName>\");\n"
			+ "\ttask->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\tkernelHash[\"<iteratorName>\"]\n"
			+ "\t\t\t->setArg(0, variablePtr->outputBuffer)\n"
			+ "<setArgs:{var|\t\t\t\t->setArg(<var.index>, <var.name>);\n}>"
			+ "\t\t\t->setWorkSize(1);\n"
			+ "\t\\});\n"
			+ "\truntimePtr->runtime->submitTask(std::move(task));\n"
			+ "\truntimePtr->runtime->finish();\n"
			+ "\t<buffers:{var|<var.name>Buffer->copyToJNI(env, <var.arrName>);\n}>"
			+ "\\}";

	/**
	 * Create the kernel file that will be used to store the user code written
	 * in the user library.
	 */
	public void createKernelFile(String className,
			IteratorsAndBinds iteratorsAndBinds,
			Map<String, UserLibraryTranslatorDefinition> translators,
			String outputDestinationFolder) {
		String templateKernelFile = "<introductoryMsg>\n"
				+ "#ifndef USERKERNELS_H\n" + "#define USERKERNELS_H\n\n"
				+ "const char userKernels[] =\n"
				+ "\t<kernels:{var|\"<var.line>\"\n}>" + "#endif\n";
		ST st = new ST(templateKernelFile);
		// 1. Add header comment
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		// 2. Translate input binds
		Set<String> inputBindTypes = new HashSet<String>();
		for (InputBind inputBind : iteratorsAndBinds.inputBinds) {
			if (!inputBindTypes.contains(inputBind.variable.typeName)) {
				inputBindTypes.add(inputBind.variable.typeName);
				String kernel = translators.get(inputBind.variable.typeName)
						.translateInputBind(className, inputBind);
				this.addKernelByLine(kernel, st);
			}
		}
		// 3. Translate iterators
		for (Iterator iterator : iteratorsAndBinds.iterators) {
			String kernel = translators.get(iterator.variable.typeName)
					.translateIterator(className, iterator);
			this.addKernelByLine(kernel, st);
		}
		// 4. Translate outputbinds
		Set<String> outputBindTypes = new HashSet<String>();
		for (OutputBind outputBind : iteratorsAndBinds.outputBinds) {
			if (!outputBindTypes.contains(outputBind.variable.typeName)) {
				outputBindTypes.add(outputBind.variable.typeName);
				String kernel = translators.get(outputBind.variable.typeName)
						.translateOutputBind(className, outputBind);
				this.addKernelByLine(kernel, st);
			}
		}
		FileWriter.writeFile(
				"userKernels.h",
				RuntimeCommonDefinitions.getInstance().getJNIDestinationFolder(
						outputDestinationFolder), st.render());
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
	 * Creates the CPP file with user defined iterators.
	 * 
	 * @param packageName
	 *            Current class package.
	 * @param className
	 *            Current class name.
	 * @param iterators
	 *            Iterators that must be created in cpp file.
	 * @param outputDestinationFolder
	 *            Destination folder for output files.
	 */
	public void createCPPFile(String packageName, String className,
			List<Iterator> iterators, String outputDestinationFolder) {
		ST st = new ST(templateCPPFile);
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		String cClassName = RuntimeCommonDefinitions.getInstance()
				.getCClassName(packageName, className);
		st.add("cClassName", cClassName);
		st.add("iterator", null);
		for (Iterator iterator : iterators) {
			if (iterator.getType() == IteratorType.Sequential) {
				st.addAggr("iterator.{body}",
						this.createSequentialIteratorBody(iterator, cClassName));
			} else {
				st.addAggr("iterator.{body}",
						this.createParallelIteratorBody(iterator, cClassName));
			}
		}
		FileWriter.writeFile(
				cClassName + ".cpp",
				RuntimeCommonDefinitions.getInstance().getJNIDestinationFolder(
						outputDestinationFolder), st.render());
	}

	private String createParallelIteratorBody(Iterator iterator,
			String cClassName) {
		ST st = new ST(templateParallelIterator);
		st.add("functionDecl",
				this.createIteratorSignature(iterator, cClassName));
		st.add("cClassName", cClassName);
		st.add("iteratorName", RuntimeCommonDefinitions.getInstance()
				.getIteratorName(iterator));
		st.add("objectType", this.getObjectType(iterator));
		return st.render();
	}

	private String createSequentialIteratorBody(Iterator iterator,
			String cClassName) {
		ST st = new ST(templateSequentialIterator);
		st.add("functionDecl",
				this.createIteratorSignature(iterator, cClassName));
		st.add("cClassName", cClassName);
		st.add("iteratorName", RuntimeCommonDefinitions.getInstance()
				.getIteratorName(iterator));
		st.add("objectType", this.getObjectType(iterator));
		int i = 0;
		for (Variable variable : iterator.getExternalVariables()) {
			String prefixedVarName = RuntimeCommonDefinitions.getInstance()
					.getPrefix() + variable.name;
			st.addAggr("buffers.{name, arrName}", variable.name,
					prefixedVarName);
			st.addAggr("setArgs.{index, name}", ++i, variable.name);
			st.addAggr("setArgs.{index, name}", ++i, variable.name + "Buffer");
		}
		if (iterator.variable.typeName.equals(HDRImage.getName())
				|| iterator.variable.typeName.equals(BitmapImage.getName())) {
			st.addAggr("setArgs.{index, name}", ++i, "variablePtr->width");
			st.addAggr("setArgs.{index, name}", ++i, "variablePtr->height");
		}

		return st.render();
	}

	private String getObjectType(Iterator iterator) {
		String objectType;
		if (iterator.variable.typeName.equals(HDRImage.getName())
				|| iterator.variable.typeName.equals(BitmapImage.getName())) {
			objectType = "ImageData";
		} else {
			objectType = "ArrayData";
		}
		return objectType;
	}

	/**
	 * Creates the H file with user defined iterators.
	 * 
	 * @param packageName
	 *            Current class package.
	 * @param className
	 *            Current class name.
	 * @param iterators
	 *            Iterators that must be created in cpp file.
	 * @param outputDestinationFolder
	 *            Destination folder for output files.
	 */
	public void createHFile(String packageName, String className,
			List<Iterator> iterators, String outputDestinationFolder) {
		String templateHFile = "<introductoryMsg>\n" + "#include \\<jni.h>\n\n"
				+ "#ifndef _Included_<cClassName>\n"
				+ "#define _Included_<cClassName>\n" + "#ifdef __cplusplus\n"
				+ "extern \"C\" {\n" + "#endif\n\n"
				+ "<iterator:{var|<var.decl>;}; separator=\"\n\n\">"
				+ "#ifdef __cplusplus\n" + "}\n" + "#endif\n" + "#endif\n";
		ST st = new ST(templateHFile);
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		String cClassName = RuntimeCommonDefinitions.getInstance()
				.getCClassName(packageName, className);
		st.add("cClassName", cClassName);
		st.add("iterator", null);
		for (Iterator iterator : iterators) {
			st.addAggr("iterator.{decl}",
					this.createIteratorSignature(iterator, cClassName));
		}
		FileWriter.writeFile(
				cClassName + ".h",
				RuntimeCommonDefinitions.getInstance().getJNIDestinationFolder(
						outputDestinationFolder), st.render());
	}

	private String createIteratorSignature(Iterator iterator, String cClassName) {
		ST st = new ST(templateIteratorFunctionDecl);
		st.add("cClassName", cClassName);
		st.add("iteratorName", RuntimeCommonDefinitions.getInstance()
				.getIteratorName(iterator));
		st.add("params", null);
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{type, name}", variable.typeName, variable.name);
			if (iterator.getType() == IteratorType.Sequential) {
				st.addAggr("params.{type, name}",
						String.format("j%sArray", variable.typeName),
						RuntimeCommonDefinitions.getInstance().getPrefix()
								+ variable.name);
			}
		}
		return st.render();
	}

}
