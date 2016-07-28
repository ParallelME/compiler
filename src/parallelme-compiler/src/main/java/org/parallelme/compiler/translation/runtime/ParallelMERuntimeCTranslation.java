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
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OperationsAndBinds;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;
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
			+ "<tileData:{var|\t\tint <var.name> = <var.expression>;\n"
			+ "\tauto <var.bufferName> = std::make_shared\\<Buffer>(<var.expression2>);\n}>"
			+ "<destinationVariable:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(<var.expression>);\n}>"
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
			+ "<destinationVariable:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(sizeof(<var.type>) * env->GetArrayLength(<var.name>));\n}>"
			+ "<buffers:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(sizeof(<var.type>));\n"
			+ "\t<var.bufferName>->setJArraySource(env, <var.arrName>);\n}>"
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

	private RuntimeCommonDefinitions commonDefinitions = RuntimeCommonDefinitions
			.getInstance();

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
		st.add("introductoryMsg", commonDefinitions.getHeaderComment());
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
		FileWriter.writeFile("userKernels.hpp", commonDefinitions
				.getJNIDestinationFolder(outputDestinationFolder), st.render());
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
		st.add("introductoryMsg", commonDefinitions.getHeaderComment());
		String cClassName = commonDefinitions.getCClassName(packageName,
				className);
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
		FileWriter.writeFile(cClassName + ".cpp", commonDefinitions
				.getJNIDestinationFolder(outputDestinationFolder), st.render());
	}

	/**
	 * Create a parallel operation function call for its equivalent kernel
	 * function.
	 * 
	 * This function is in protected scope in order to be called by unit tests.
	 * 
	 * @param operation
	 *            Operation that will have the function created.
	 * @param className
	 *            Current class name.
	 * @return A string with a complete function (including signature) to be
	 *         inserted in the CPP file.
	 */
	protected String createParallelOperation(Operation operation,
			String cClassName) {
		ST st = new ST(templateParallelOperation);
		st.add("functionDecl",
				this.createOperationSignature(operation, cClassName, true));
		String operationName = commonDefinitions.getOperationName(operation);
		st.add("objectType", this.getObjectType(operation));
		st.add("tileData", null);
		st.add("destinationVariable", null);
		if (operation.operationType == OperationType.Foreach) {
			fillParallelForeach(st, operation);
		} else if (operation.operationType == OperationType.Map) {
		} else if (operation.operationType == OperationType.Reduce) {
			fillParallelReduce(st, operation);
		} else if (operation.operationType == OperationType.Filter) {
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		st.addAggr("task.{operationName}", operationName);
		return st.render();
	}

	private void fillParallelForeach(ST st, Operation operation) {
		int argIndex = 0;
		ST stKernelHash = new ST(templateKernelHash);
		stKernelHash.add("setArgs", null);
		stKernelHash.add("operationName",
				commonDefinitions.getOperationName(operation));
		stKernelHash.add("tileData", null);
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				"variablePtr->" + getOperationBufferName(operation));
		if (commonDefinitions.isImage(operation.variable)) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getPointerWidth());
			stKernelHash.add("workSize", getPointerWidth() + ", "
					+ getPointerHeight());
		} else {
			stKernelHash.add("workSize", "variablePtr->workSize");
		}
		setExternalVariables(stKernelHash, operation, argIndex);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
	}

	private void fillParallelReduce(ST st, Operation operation) {
		boolean isImage = commonDefinitions.isImage(operation.variable);
		String operationTileName = commonDefinitions
				.getOperationTileFunctionName(operation);
		// Kernel hash for tile function
		ST stKernelHashTile = new ST(templateKernelHash);
		int argIndex = 0;
		String tileVarBuffer = "tileBuffer";
		String pointerVarBuffer = "variablePtr->"
				+ getOperationBufferName(operation);
		stKernelHashTile.add("operationName", operationTileName);
		stKernelHashTile.addAggr("setArgs.{index, name}", argIndex++,
				pointerVarBuffer);
		stKernelHashTile.addAggr("setArgs.{index, name}", argIndex++,
				tileVarBuffer);
		String tileSizeVar = isImage ? "tileElemSize" : "tileSize";
		if (isImage) {
			stKernelHashTile.addAggr("setArgs.{index, name}", argIndex++,
					getPointerWidth());
			stKernelHashTile.add("workSize", getPointerHeight());
		} else {
			stKernelHashTile.addAggr("setArgs.{index, name}", argIndex++,
					tileSizeVar);
			stKernelHashTile.add("workSize", tileSizeVar);
		}
		setExternalVariables(stKernelHashTile, operation, argIndex);
		st.addAggr("kernelHash.{body}", stKernelHashTile.render());
		// Kernel hash for base function
		ST stKernelHash = new ST(templateKernelHash);
		argIndex = 0;
		String destVarName = operation.destinationVariable.name;
		String destVarBuffer = destVarName + "Buffer";
		stKernelHash.add("operationName",
				commonDefinitions.getOperationName(operation));
		stKernelHash
				.addAggr("setArgs.{index, name}", argIndex++, destVarBuffer);
		if (isImage) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					tileVarBuffer);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getPointerHeight());
		} else {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					pointerVarBuffer);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					tileVarBuffer);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getPointerLength());
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					tileSizeVar);
		}
		stKernelHash.add("workSize", "1");
		setExternalVariables(stKernelHash, operation, argIndex);

		String returnType = getJNIType(operation);
		st.addAggr("task.{operationName}", operationTileName);
		String expression, expression2, expression3;
		if (isImage) {
			expression = String.format(
					"sizeof(float) * env->GetArrayLength(%s)", destVarName);
			expression2 = String
					.format("%s * variablePtr->height", tileSizeVar);
			expression3 = tileSizeVar;
		} else {
			expression = String.format("floor(sqrt((float)%s))",
					getPointerLength());
			expression2 = String.format("sizeof(%s) * %s", returnType,
					tileSizeVar);
			expression3 = String.format("sizeof(%s)", returnType);
		}
		st.addAggr(
				"tileData.{bufferName, type, name, expression, expression2}",
				tileVarBuffer, returnType, tileSizeVar, expression, expression2);
		st.addAggr("destinationVariable.{bufferName, name, type, expression}",
				destVarBuffer, destVarName, returnType, expression3);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
	}

	private void setExternalVariables(ST st, Operation operation, int argIndex) {
		for (Variable variable : operation.getExternalVariables()) {
			if (variable.isFinal()) {
				st.addAggr("setArgs.{index, name}", argIndex++, variable.name);
			} else {
				st.addAggr("setArgs.{index, name}", argIndex++,
						commonDefinitions.getPrefix() + variable.name);
			}
		}
	}

	/**
	 * Create a sequential operation function call for its equivalent kernel
	 * function.
	 * 
	 * This function is in protected scope in order to be called by unit tests.
	 * 
	 * @param operation
	 *            Operation that will have the function created.
	 * @param className
	 *            Current class name.
	 * @return A string with a complete function (including signature) to be
	 *         inserted in the CPP file.
	 */
	protected String createSequentialOperation(Operation operation,
			String cClassName) {
		ST st = new ST(templateSequentialOperation);
		st.add("functionDecl",
				this.createOperationSignature(operation, cClassName, true));
		st.add("cClassName", cClassName);
		String operationName = commonDefinitions.getOperationName(operation);
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
		if (commonDefinitions.isImage(operation.variable)) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getPointerWidth());
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getPointerHeight());
		} else {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getPointerLength());
		}
		for (Variable variable : operation.getExternalVariables()) {
			if (!variable.isFinal()) {
				String prefixedVarName = commonDefinitions.getPrefix()
						+ variable.name;
				String bufferName = variable.name + "Buffer";
				st.addAggr("buffers.{bufferName, type, arrName}", bufferName,
						commonDefinitions.translateToCType(variable.typeName),
						prefixedVarName);
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
		st.add("introductoryMsg", commonDefinitions.getHeaderComment());
		String cClassName = commonDefinitions.getCClassName(packageName,
				className);
		st.add("cClassName", cClassName);
		st.add("operation", null);
		for (Operation operation : operations) {
			st.addAggr("operation.{decl}",
					this.createOperationSignature(operation, cClassName, false));
		}
		FileWriter.writeFile(cClassName + ".h", commonDefinitions
				.getJNIDestinationFolder(outputDestinationFolder), st.render());
	}

	private String createOperationSignature(Operation operation,
			String cClassName, boolean declareVarNames) {
		ST st = new ST(templateOperationFunctionDecl);
		st.add("cClassName", cClassName);
		st.add("operationName", commonDefinitions.getOperationName(operation));
		st.add("params", null);
		if (!declareVarNames)
			st.add("varName", null);
		else
			st.add("varName", " ");
		for (Variable variable : operation.getExternalVariables()) {
			if (variable.isFinal()) {
				String decl = variable.typeName;
				if (declareVarNames)
					decl += " " + variable.name;
				st.addAggr("params.{decl}", decl);
			} else {
				String decl = String.format("j%sArray", variable.typeName);
				if (declareVarNames)
					decl += " " + commonDefinitions.getPrefix() + variable.name;
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
		if (commonDefinitions.isImage(operation.variable)) {
			type = commonDefinitions
					.translateToCType(operation.variable.typeName);
		} else {
			type = commonDefinitions
					.translateToCType(operation.destinationVariable.typeName);
		}
		return type;
	}

	private String getObjectType(Operation operation) {
		String objectType;
		if (commonDefinitions.isImage(operation.variable)) {
			objectType = "ImageData";
		} else {
			objectType = "ArrayData";
		}
		return objectType;
	}

	private String getOperationBufferName(Operation operation) {
		String bufferName;
		if (commonDefinitions.isImage(operation.variable)) {
			bufferName = "outputBuffer";
		} else {
			bufferName = "buffer";
		}
		return bufferName;
	}

	private String getPointerWidth() {
		return "variablePtr->width";
	}

	private String getPointerHeight() {
		return "variablePtr->height";
	}

	private String getPointerLength() {
		return "variablePtr->length";
	}
}
