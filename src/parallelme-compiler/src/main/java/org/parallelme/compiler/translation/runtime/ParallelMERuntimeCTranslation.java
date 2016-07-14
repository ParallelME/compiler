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
	private static final String templateKernelHash = "kernelHash[\"<operationName>\"]\n"
			+ "<setArgs:{var|\t\t->setArg(<var.index>, <var.name>)\n}>"
			+ "\t->setWorkSize(<workSize>);";
	private static final String templateParallelOperation = "<functionDecl> {\n"
			+ "\tauto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
			+ "\tauto variablePtr = (<objectType> *) varPtr;\n"
			+ "\tauto task = std::make_unique\\<Task>(runtimePtr->program);\n"
			+ "<destinationVariable:{var|\t\tauto tileElemSize = sizeof(<var.type>) * env->GetArrayLength(<var.name>);\n"
			+ "\tauto tileBuffer = std::make_shared\\<Buffer>(tileElemSize * <var.expression>);\n"
			+ "\tauto <var.bufferName> = std::make_shared\\<Buffer>(tileElemSize);\n}>"
			+ "<task:{var|\t\ttask->addKernel(\"<var.operationName>\");\n}>"
			+ "\ttask->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\t<kernelHash:{var|<var.body>}; separator=\"\n\">"
			+ "\n\t\\});\n"
			+ "\truntimePtr->runtime->submitTask(std::move(task));\n"
			+ "\truntimePtr->runtime->finish();\n"
			+ "<destinationVariable:{var|\t\t<var.bufferName>->copyToJArray(env, <var.name>);\n}>"
			+ "\\}";
	private static final String templateSequentialOperation = "<functionDecl> {\n"
			+ "\tauto runtimePtr = (ParallelMERuntimeData *) rtmPtr;\n"
			+ "\tauto variablePtr = (<objectType> *) varPtr;\n"
			+ "\tauto task = std::make_unique\\<Task>(runtimePtr->program, Task::Score(1.0f, 2.0f));\n"
			+ "<destinationVariable:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(sizeof(<var.type>) * GetArrayLength(env, <var.name>));\n}>"
			+ "<buffers:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(sizeof(<var.name>));\n}>"
			+ "<task:{var|\t\ttask->addKernel(\"<var.operationName>\");\n}>"
			+ "\ttask->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\t<kernelHash:{var|<var.body>}; separator=\"\n\">"
			+ "\n\t\\});\n"
			+ "\truntimePtr->runtime->submitTask(std::move(task));\n"
			+ "\truntimePtr->runtime->finish();\n"
			+ "<buffers:{var|\t\t<var.bufferName>->copyToJArray(env, <var.arrName>);\n}>"
			+ "<destinationVariable:{var|\t\t<var.bufferName>->copyToJArray(env, <var.name>);\n}>"
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
			List<String> kernels = translators.get(operation.variable.typeName)
					.translateOperation(operation);
			for (String kernel : kernels)
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
				st.addAggr("operation.{body}",
						createSequentialOperation(operation, cClassName));
			} else {
				st.addAggr("operation.{body}",
						createParallelOperation(operation, cClassName));
			}
		}
		FileWriter.writeFile(
				cClassName + ".cpp",
				RuntimeCommonDefinitions.getInstance().getJNIDestinationFolder(
						outputDestinationFolder), st.render());
	}

	protected String createParallelOperation(Operation operation,
			String cClassName) {
		ST st = new ST(templateParallelOperation);
		st.add("functionDecl",
				this.createOperationSignature(operation, cClassName, true));
		st.add("cClassName", cClassName);
		String operationName = RuntimeCommonDefinitions.getInstance()
				.getOperationName(operation);
		String operationTileName = RuntimeCommonDefinitions.getInstance()
				.getOperationTileFunctionName(operation);
		st.add("operationName", operationName);
		st.add("objectType", this.getObjectType(operation));
		ST stKernelHashTile = new ST(templateKernelHash);
		ST stKernelHash = new ST(templateKernelHash);
		stKernelHash.add("setArgs", null);
		stKernelHashTile.add("setArgs", null);
		stKernelHash.add("operationName", operationName);
		stKernelHashTile.add("operationName", operationTileName);
		int argIndex = 0;
		String operationBufferName = this.getOperationBufferName(operation);
		stKernelHashTile.addAggr("setArgs.{index, name}", argIndex,
				"variablePtr->" + operationBufferName);
		if (operation.destinationVariable != null) {
			String destVarName = operation.destinationVariable.name;
			String type = this.getJNIType(operation);
			String bufferName = destVarName + "Buffer";
			String expression = this.getDestinationBufferMultiFactor(operation);
			st.addAggr(
					"destinationVariable.{bufferName, name, type, expression}",
					bufferName, destVarName, type, expression);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex, bufferName);
			stKernelHashTile.addAggr("setArgs.{index, name}", ++argIndex,
					"tileBuffer");
			stKernelHash.addAggr("setArgs.{index, name}", argIndex,
					"tileBuffer");
			st.addAggr("task.{operationName}", operationTileName);
			if (operation.variable.typeName.equals(BitmapImage.getInstance()
					.getClassName())
					|| operation.variable.typeName.equals(HDRImage
							.getInstance().getClassName())) {
				stKernelHashTile.addAggr("setArgs.{index, name}", ++argIndex,
						"variablePtr->width");
				stKernelHash.addAggr("setArgs.{index, name}", argIndex,
						"variablePtr->height");
				stKernelHashTile.add("workSize", "variablePtr->height");
				stKernelHash.add("workSize", "1");
			} else {
				stKernelHashTile.add("workSize",
						"floor(sqrt((float)variablePtr->length))");
				stKernelHash.add("workSize", "1");
			}
		} else {
			st.add("destinationVariable", null);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex,
					"variablePtr->" + operationBufferName);
			stKernelHash.add("workSize", "variablePtr->workSize");
		}
		for (Variable variable : operation.getExternalVariables()) {
			stKernelHash.addAggr("setArgs.{index, name}", ++argIndex,
					variable.name);
			stKernelHashTile.addAggr("setArgs.{index, name}", argIndex,
					variable.name);
		}
		if (operation.destinationVariable != null)
			st.addAggr("kernelHash.{body}", stKernelHashTile.render());
		st.addAggr("task.{operationName}", operationName);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
		return st.render();
	}

	/**
	 * Returns the multiplication factor that is necessary when creating a
	 * buffer for destination variables.
	 */
	private String getDestinationBufferMultiFactor(Operation operation) {
		if (operation.variable.typeName.equals(BitmapImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(HDRImage.getInstance()
						.getClassName())) {
			return "variablePtr->height";
		} else {
			// TODO
			return "floor(sqrt(vector size))";
		}
	}

	protected String createSequentialOperation(Operation operation,
			String cClassName) {
		ST st = new ST(templateSequentialOperation);
		st.add("functionDecl",
				this.createOperationSignature(operation, cClassName, true));
		st.add("cClassName", cClassName);
		String operationName = RuntimeCommonDefinitions.getInstance()
				.getOperationName(operation);
		st.add("operationName", operationName);
		st.add("objectType", this.getObjectType(operation));
		st.addAggr("task.{operationName}", operationName);
		ST stKernelHash = new ST(templateKernelHash);
		stKernelHash.add("workSize", "1");
		stKernelHash.add("operationName", operationName);
		stKernelHash.add("setArgs", null);
		st.add("buffers", null);
		int argIndex = 0;
		if (operation.destinationVariable != null) {
			String destVarName = operation.destinationVariable.name;
			String type = this.getJNIType(operation);
			String bufferName = destVarName + "Buffer";
			st.addAggr("destinationVariable.{bufferName, name, type}",
					bufferName, destVarName, type);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					bufferName);
		} else {
			st.add("destinationVariable", null);
		}
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				"variablePtr->" + this.getOperationBufferName(operation));
		if (operation.variable.typeName.equals(HDRImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(BitmapImage.getInstance()
						.getClassName())) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					"variablePtr->width");
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					"variablePtr->height");
		} else {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					"variablePtr->length");
		}
		for (Variable variable : operation.getExternalVariables()) {
			if (!variable.isFinal()) {
				String prefixedVarName = RuntimeCommonDefinitions.getInstance()
						.getPrefix() + variable.name;
				String bufferName = variable.name + "Buffer";
				st.addAggr("buffers.{bufferName, name, arrName}", bufferName,
						variable.name, prefixedVarName);
				stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
						variable.name);
				stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
						bufferName);
			} else {
				stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
						variable.name);
			}
		}
		st.addAggr("kernelHash.{body}", stKernelHash.render());
		return st.render();
	}

	private String getObjectType(Operation operation) {
		String objectType;
		if (operation.variable.typeName.equals(HDRImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(BitmapImage.getInstance()
						.getClassName())) {
			objectType = "ImageData";
		} else {
			objectType = "ArrayData";
		}
		return objectType;
	}

	private String getOperationBufferName(Operation operation) {
		String bufferName;
		if (operation.variable.typeName.equals(HDRImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(BitmapImage.getInstance()
						.getClassName())) {
			bufferName = "outputBuffer";
		} else {
			bufferName = "buffer";
		}
		return bufferName;
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
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		for (Variable variable : operation.getExternalVariables()) {
			String decl = variable.typeName;
			if (declareVarNames)
				decl += " " + variable.name;
			st.addAggr("params.{decl}", decl);
			if (isSequential && !variable.isFinal()) {
				decl = String.format("j%sArray", variable.typeName);
				if (declareVarNames)
					decl += " "
							+ RuntimeCommonDefinitions.getInstance()
									.getPrefix() + variable.name;
				st.addAggr("params.{decl}", decl);
			}
		}
		if (operation.destinationVariable != null) {
			String decl;
			String typeName = this.getJNIType(operation);
			decl = String.format("j%sArray", typeName);
			if (declareVarNames)
				decl += " " + operation.destinationVariable.name;
			st.addAggr("params.{decl}", decl);
		}
		return st.render();
	}

	/**
	 * Return the JNI-equivalent type for a given operation.
	 */
	private String getJNIType(Operation operation) {
		String type;
		if (operation.variable.typeName.equals(HDRImage.getInstance()
				.getClassName())
				|| operation.variable.typeName.equals(BitmapImage.getInstance()
						.getClassName())) {
			type = RuntimeCommonDefinitions.getInstance().translateType(
					operation.variable.typeName);
		} else {
			type = RuntimeCommonDefinitions.getInstance().translateType(
					operation.destinationVariable.typeName);
		}
		return type;
	}
}
