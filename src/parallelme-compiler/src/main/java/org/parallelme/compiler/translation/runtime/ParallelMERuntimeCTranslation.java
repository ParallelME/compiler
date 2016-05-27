/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OperationsAndBinds;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;
import org.parallelme.compiler.userlibrary.classes.Array;
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
	private static final String templateCPPFile = "<introductoryMsg>\n\n"
			+ "#include \"<cClassName>.h\"\n\n" + "#include \\<memory>\n"
			+ "#include \\<stdexcept>\n" + "#include \\<android/log.h>\n"
			+ "#include \\<parallelme/ParallelME.hpp>\n"
			+ "#include \\<parallelme/SchedulerHEFT.hpp>\n"
			+ "#include \"ParallelMEData.hpp\"\n\n"
			+ "using namespace parallelme;\n\n"
			+ "<operation:{var|<var.body>}; separator=\"\n\n\">";
	private static final String templateHFile = "<introductoryMsg>\n\n"
			+ "#include \\<jni.h>\n\n" + "#ifndef _Included_<cClassName>\n"
			+ "#define _Included_<cClassName>\n" + "#ifdef __cplusplus\n"
			+ "extern \"C\" {\n" + "#endif\n\n"
			+ "<operation:{var|<var.decl>;}; separator=\"\n\n\">"
			+ "\n\n#ifdef __cplusplus\n" + "}\n" + "#endif\n" + "#endif\n";
	private static final String templateOperationFunctionDecl = "JNIEXPORT void JNICALL Java_<cClassName>_<operationName>\n"
			+ "\t\t(JNIEnv *<varName:{var|env}>, jobject <varName:{var|self}>, jlong <varName:{var|rtmPtr}>, jlong <varName:{var|varPtr}><params:{var|, <var.decl>}>)";
	private static final String templateParallelOperation = "<functionDecl> {\n"
			+ "\tauto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
			+ "\tauto variablePtr = (<objectType> *) varPtr;\n"
			+ "\tauto task = std::make_unique\\<Task>(runtimePtr->program);\n"
			+ "\ttask->addKernel(\"<operationName>\");\n"
			+ "\ttask->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\tkernelHash[\"<operationName>\"]\n"
			+ "\t\t\t->setArg(0, variablePtr->outputBuffer)\n"
			+ "<setArgs:{var|\t\t\t\t->setArg(<var.index>, <var.name>)\n}>"
			+ "\t\t\t->setWorkSize(variablePtr->workSize);\n"
			+ "\t\\});\n"
			+ "\truntimePtr->runtime->submitTask(std::move(task));\n"
			+ "\truntimePtr->runtime->finish();\n" + "\\}";
	private static final String templateSequentialOperation = "<functionDecl> {\n"
			+ "\tauto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
			+ "\tauto variablePtr = (<objectType> *) varPtr;\n"
			+ "\t<buffers:{var|auto <var.name>Buffer = std::make_shared\\<Buffer>(sizeof(<var.name>));\n}>"
			+ "\tauto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f, 2.0f));\n"
			+ "\ttask->addKernel(\"<operationName>\");\n"
			+ "\ttask->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\tkernelHash[\"<operationName>\"]\n"
			+ "\t\t\t->setArg(0, variablePtr->outputBuffer)\n"
			+ "<setArgs:{var|\t\t\t\t->setArg(<var.index>, <var.name>)\n}>"
			+ "\t\t\t->setWorkSize(1);\n"
			+ "\t\\});\n"
			+ "\truntimePtr->runtime->submitTask(std::move(task));\n"
			+ "\truntimePtr->runtime->finish();\n"
			+ "\t<buffers:{var|<var.name>Buffer->copyToJNI(env, <var.arrName>);\n}>"
			+ "\\}";
	private final static String templateKernelFile = "<introductoryMsg>\n\n"
			+ "#ifndef USERKERNELS_HPP\n" + "#define USERKERNELS_HPP\n\n"
			+ "const char userKernels[] =\n"
			+ "\t<kernels:{var|\"<var.line>\"}; separator=\"\n\">;\n"
			+ "#endif\n";

	/**
	 * Create the kernel file that will be used to store the user code written
	 * in the user library.
	 */
	public void createKernelFile(String className,
			OperationsAndBinds operationsAndBinds,
			Map<String, UserLibraryTranslatorDefinition> translators,
			String outputDestinationFolder) {
		ST st = new ST(templateKernelFile);
		// 1. Add header comment
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		// 2. Translate input binds
		Set<String> inputBindTypes = new HashSet<String>();
		for (InputBind inputBind : operationsAndBinds.inputBinds) {
			if (!inputBindTypes.contains(inputBind.variable.typeName)) {
				inputBindTypes.add(inputBind.variable.typeName);
				String kernel = translators.get(inputBind.variable.typeName)
						.translateInputBind(className, inputBind);
				this.addKernelByLine(kernel, st);
			}
		}
		// 3. Translate operations
		for (Operation operation : operationsAndBinds.operations) {
			String kernel = translators.get(operation.variable.typeName)
					.translateOperation(className, operation);
			this.addKernelByLine(kernel, st);
		}
		// 4. Translate outputbinds
		Set<String> outputBindTypes = new HashSet<String>();
		for (OutputBind outputBind : operationsAndBinds.outputBinds) {
			if (!outputBindTypes.contains(outputBind.variable.typeName)) {
				outputBindTypes.add(outputBind.variable.typeName);
				String kernel = translators.get(outputBind.variable.typeName)
						.translateOutputBind(className, outputBind);
				this.addKernelByLine(kernel, st);
			}
		}
		FileWriter.writeFile(
				"userKernels.hpp",
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
	 * Creates the CPP file with user defined operations.
	 * 
	 * @param packageName
	 *            Current class package.
	 * @param className
	 *            Current class name.
	 * @param operations
	 *            Operations that must be created in cpp file.
	 * @param outputDestinationFolder
	 *            Destination folder for output files.
	 */
	public void createCPPFile(String packageName, String className,
			List<Operation> operations, String outputDestinationFolder) {
		ST st = new ST(templateCPPFile);
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		String cClassName = RuntimeCommonDefinitions.getInstance()
				.getCClassName(packageName, className);
		st.add("cClassName", cClassName);
		st.add("operation", null);
		for (Operation operation : operations) {
			if (operation.getExecutionType() == ExecutionType.Sequential) {
				st.addAggr("operation.{body}", this
						.createSequentialOperationBody(operation, cClassName));
			} else {
				st.addAggr("operation.{body}",
						this.createParallelOperationBody(operation, cClassName));
			}
		}
		FileWriter.writeFile(
				cClassName + ".cpp",
				RuntimeCommonDefinitions.getInstance().getJNIDestinationFolder(
						outputDestinationFolder), st.render());
	}

	private String createParallelOperationBody(Operation operation,
			String cClassName) {
		ST st = new ST(templateParallelOperation);
		st.add("functionDecl",
				this.createOperationSignature(operation, cClassName, true));
		st.add("cClassName", cClassName);
		st.add("operationName", RuntimeCommonDefinitions.getInstance()
				.getOperationName(operation));
		st.add("objectType", this.getObjectType(operation));
		st.add("setArgs", null);
		int i = 0;
		for (Variable variable : operation.getExternalVariables()) {
			st.addAggr("setArgs.{index, name}", ++i, variable.name);
		}
		return st.render();
	}

	private String createSequentialOperationBody(Operation operation,
			String cClassName) {
		ST st = new ST(templateSequentialOperation);
		st.add("functionDecl",
				this.createOperationSignature(operation, cClassName, true));
		st.add("cClassName", cClassName);
		st.add("operationName", RuntimeCommonDefinitions.getInstance()
				.getOperationName(operation));
		st.add("objectType", this.getObjectType(operation));
		st.add("setArgs", null);
		int i = 0;
		for (Variable variable : operation.getExternalVariables()) {
			String prefixedVarName = RuntimeCommonDefinitions.getInstance()
					.getPrefix() + variable.name;
			st.addAggr("buffers.{name, arrName}", variable.name,
					prefixedVarName);
			st.addAggr("setArgs.{index, name}", ++i, variable.name);
			st.addAggr("setArgs.{index, name}", ++i, variable.name + "Buffer");
		}
		if (operation.variable.typeName.equals(HDRImage.getName())
				|| operation.variable.typeName.equals(BitmapImage.getName())) {
			st.addAggr("setArgs.{index, name}", ++i, "variablePtr->width");
			st.addAggr("setArgs.{index, name}", ++i, "variablePtr->height");
		} else if (operation.variable.typeName.equals(Array.getName())) {
			st.addAggr("setArgs.{index, name}", ++i, "variablePtr->length");
		}

		return st.render();
	}

	private String getObjectType(Operation operation) {
		String objectType;
		if (operation.variable.typeName.equals(HDRImage.getName())
				|| operation.variable.typeName.equals(BitmapImage.getName())) {
			objectType = "ImageData";
		} else {
			objectType = "ArrayData";
		}
		return objectType;
	}

	/**
	 * Creates the H file with user defined operations.
	 * 
	 * @param packageName
	 *            Current class package.
	 * @param className
	 *            Current class name.
	 * @param operations
	 *            Operations that must be created in cpp file.
	 * @param outputDestinationFolder
	 *            Destination folder for output files.
	 */
	public void createHFile(String packageName, String className,
			List<Operation> operations, String outputDestinationFolder) {
		ST st = new ST(templateHFile);
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		String cClassName = RuntimeCommonDefinitions.getInstance()
				.getCClassName(packageName, className);
		st.add("cClassName", cClassName);
		st.add("operation", null);
		for (Operation operation : operations) {
			st.addAggr("operation.{decl}",
					this.createOperationSignature(operation, cClassName, false));
		}
		FileWriter.writeFile(
				cClassName + ".h",
				RuntimeCommonDefinitions.getInstance().getJNIDestinationFolder(
						outputDestinationFolder), st.render());
	}

	private String createOperationSignature(Operation operation,
			String cClassName, boolean declareVarNames) {
		ST st = new ST(templateOperationFunctionDecl);
		st.add("cClassName", cClassName);
		st.add("operationName", RuntimeCommonDefinitions.getInstance()
				.getOperationName(operation));
		st.add("params", null);
		if (!declareVarNames)
			st.add("varName", null);
		else
			st.add("varName", " ");
		for (Variable variable : operation.getExternalVariables()) {
			String decl = variable.typeName;
			if (declareVarNames)
				decl += " " + variable.name;
			st.addAggr("params.{decl}", decl);
			if (operation.getExecutionType() == ExecutionType.Sequential) {
				decl = String.format("j%sArray", variable.typeName);
				if (declareVarNames)
					decl += " "
							+ RuntimeCommonDefinitions.getInstance()
									.getPrefix() + variable.name;
				st.addAggr("params.{decl}", decl);
			}
		}
		return st.render();
	}

}
