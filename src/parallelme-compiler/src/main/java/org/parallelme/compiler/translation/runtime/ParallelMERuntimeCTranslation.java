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
			+ "#include \"ParallelMEData.hpp\"\n"
			+ "#include \"org_parallelme_ParallelMERuntime.h\"\n\n"
			+ "using namespace parallelme;\n\n"
			+ "<operation:{var|<var.body>}; separator=\"\n\n\">";
	private static final String templateHFile = "<introductoryMsg>\n\n"
			+ "#include \\<jni.h>\n\n" + "#ifndef _Included_<cClassName>\n"
			+ "#define _Included_<cClassName>\n" + "#ifdef __cplusplus\n"
			+ "extern \"C\" {\n" + "#endif\n\n"
			+ "<operation:{var|<var.decl>;}; separator=\"\n\n\">"
			+ "\n\n#ifdef __cplusplus\n" + "}\n" + "#endif\n" + "#endif\n";
	private static final String templateOperationFunctionDecl = "JNIEXPORT <returnType> JNICALL Java_<cClassName>_<operationName>\n"
			+ "\t\t(JNIEnv *<varName:{var|env}>, jobject <varName:{var|self}>, jlong <varName:{var|PM_runtime}>, jlong <varName:{var|PM_data}><params:{var|, <var.decl>}>)";
	private static final String templateKernelHash = "kernelHash[\"<operationName>\"]\n"
			+ "<setArgs:{var|\t\t->setArg(<var.index>, <var.name>)\n}>"
			+ "\t->setWorkSize(<workSize>);";
	private static final String templateParallelOperationBody = "\tauto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
			+ "\tauto PM_dataPtr = (<objectType> *) PM_data;\n"
			+ "<returnPointer:{var|\t\tauto PM_dataRetPtr = (<var.objectType> *) <var.name>;\n}>"
			+ "\tauto <taskName> = std::make_unique\\<Task>(PM_runtimePtr->program);\n"
			+ "<tileData:{var|\t\tint <var.name> = <var.expression>;\n}>"
			+ "<buffers:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(<var.expression>);\n}>"
			+ "<destinationVariable:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(<var.expression>);\n}>"
			+ "<task:{var|\t\t<taskName>->addKernel(\"<var.operationName>\");\n}>"
			+ "\t<taskName>->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\t<kernelHash:{var|<var.body>}; separator=\"\n\">"
			+ "\n\t\\});\n"
			+ "\tPM_runtimePtr->runtime->submitTask(std::move(<taskName>));\n"
			+ "\tPM_runtimePtr->runtime->finish();\n"
			+ "<destinationVariable:{var|\t\t<var.bufferName>->copyToJArray(env, <var.name>);\n}>";
	private static final String templateSequentialOperationBody = "\tauto PM_runtimePtr = (ParallelMERuntimeData *) PM_runtime;\n"
			+ "\tauto PM_dataPtr = (<objectType> *) PM_data;\n"
			+ "<returnPointer:{var|\t\tauto PM_dataRetPtr = (<var.objectType> *) <var.name>;\n}>"
			+ "\tauto <taskName> = std::make_unique\\<Task>(PM_runtimePtr->program, Task::Score(1.0f, 2.0f));\n"
			+ "<destinationVariable:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(<var.expression>);\n}>"
			+ "<buffers:{var|\t\tauto <var.bufferName> = std::make_shared\\<Buffer>(<var.expression>);\n"
			+ "<var.setArrayBuffer:{var2|\t\t<var.bufferName>->setJArraySource(env, <var.arrName>);\n}>}>"
			+ "<task:{var|\t\t<taskName>->addKernel(\"<var.operationName>\");\n}>"
			+ "\t<taskName>->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\t<kernelHash:{var|<var.body>}; separator=\"\n\">"
			+ "\n\t\\});\n"
			+ "\tPM_runtimePtr->runtime->submitTask(std::move(<taskName>));\n"
			+ "\tPM_runtimePtr->runtime->finish();\n"
			+ "<buffers:{var|<var.setArrayBuffer:{var2|\t\t<var.bufferName>->copyToJArray(env, <var.arrName>);\n}>}>"
			+ "<destinationVariable:{var|\t\t<var.bufferName>->copyToJArray(env, <var.name>);\n}>";
	private final static String templateKernelFile = "<introductoryMsg>\n\n"
			+ "#ifndef USERKERNELS_HPP\n" + "#define USERKERNELS_HPP\n\n"
			+ "const char userKernels[] =\n"
			+ "\t<kernels:{var|\"<var.line>\"}; separator=\"\n\">;\n"
			+ "#endif\n";
	private final static String templateFilter = "\tjintArray PM_tileArray = env->NewIntArray(<tileSize>);\n"
			+ "\t<bufferName>->copyToJArray(env, PM_tileArray);\n"
			+ "\tauto <taskName> = std::make_unique\\<Task>(PM_runtimePtr->program<isSequential:{var|, Task::Score(1.0f,2.0f)}>);\n"
			+ "\tint PM_length = getFilterArrayLength(env, PM_tileArray);\n"
			+ "\tauto <retVar> = (ArrayData *)Java_org_parallelme_ParallelMERuntime_nativeCreateArray__II(env, self, PM_length, 2);\n"
			+ "<task:{var|\t\t<taskName>->addKernel(\"<var.operationName>\");\n}>"
			+ "\t<taskName>->setConfigFunction([=](DevicePtr &device, KernelHash &kernelHash) {\n"
			+ "\t\t<kernelHash:{var|<var.body>}; separator=\"\n\">"
			+ "\n\t\\});\n"
			+ "\tPM_runtimePtr->runtime->submitTask(std::move(<taskName>));\n"
			+ "\tPM_runtimePtr->runtime->finish();\n"
			+ "\treturn (jlong)<retVar>;\n";

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
				addKernelByLine(kernel, st);
			}
		}
		// 3. Translate operations
		for (Operation operation : operationsAndBinds.operations) {
			List<String> kernels = translators.get(operation.variable.typeName)
					.translateOperation(operation);
			for (String kernel : kernels)
				addKernelByLine(kernel, st);
		}
		// 4. Translate outputbinds
		Set<String> outputBindTypes = new HashSet<String>();
		for (OutputBind outputBind : operationsAndBinds.outputBinds) {
			if (!outputBindTypes.contains(outputBind.variable.typeName)) {
				outputBindTypes.add(outputBind.variable.typeName);
				String kernel = translators.get(outputBind.variable.typeName)
						.translateOutputBind(className, outputBind);
				addKernelByLine(kernel, st);
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
		String function;
		if (operation.operationType == OperationType.Foreach) {
			function = createParallelForeach(operation);
		} else if (operation.operationType == OperationType.Map) {
			function = createParallelMap(operation);
		} else if (operation.operationType == OperationType.Reduce) {
			function = createParallelReduce(operation);
		} else if (operation.operationType == OperationType.Filter) {
			function = createFilter(operation);
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		return createOperationSignature(operation, cClassName, true) + " {\n"
				+ function + "}";
	}

	private String createParallelForeach(Operation operation) {
		ST st = initializeParallelOperationBody(operation);
		int argIndex = 0;
		ST stKernelHash = new ST(templateKernelHash);
		stKernelHash.add("setArgs", null);
		stKernelHash.add("operationName",
				commonDefinitions.getOperationName(operation));
		stKernelHash.add("tileData", null);
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				commonDefinitions.getDataVarName() + "Ptr->"
						+ getOperationBufferName(operation.variable));
		if (commonDefinitions.isImage(operation.variable)) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerWidth());
			stKernelHash.add("workSize", getDataPointerWidth() + ", "
					+ getDataPointerHeight());
		} else {
			stKernelHash.add("workSize", getDataPointerLength());
		}
		setExternalVariables(stKernelHash, operation, argIndex);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
		st.addAggr("task.{operationName}",
				commonDefinitions.getOperationName(operation));
		return st.render();
	}

	private String createParallelMap(Operation operation) {
		ST st = initializeParallelOperationBody(operation);
		int argIndex = 0;
		ST stKernelHash = new ST(templateKernelHash);
		stKernelHash.add("setArgs", null);
		stKernelHash.add("operationName",
				commonDefinitions.getOperationName(operation));
		stKernelHash.add("tileData", null);
		stKernelHash
				.addAggr(
						"setArgs.{index, name}",
						argIndex++,
						commonDefinitions.getDataReturnVarName()
								+ "Ptr->"
								+ getOperationBufferName(operation.destinationVariable));
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				commonDefinitions.getDataVarName() + "Ptr->"
						+ getOperationBufferName(operation.variable));
		if (commonDefinitions.isImage(operation.variable)) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerWidth());
			stKernelHash.add("workSize", getDataPointerWidth() + ", "
					+ getDataPointerHeight());
		} else {
			stKernelHash.add("workSize", getDataPointerLength());
		}
		st.addAggr("returnPointer.{objectType, name}", "ArrayData",
				commonDefinitions.getDataReturnVarName());
		setExternalVariables(stKernelHash, operation, argIndex);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
		st.addAggr("task.{operationName}",
				commonDefinitions.getOperationName(operation));
		return st.render();
	}

	private String createParallelReduce(Operation operation) {
		ST st = initializeParallelOperationBody(operation);
		boolean isImage = commonDefinitions.isImage(operation.variable);
		String operationTileName = commonDefinitions
				.getOperationTileFunctionName(operation);
		// Kernel hash for tile function
		ST stKernelHashTile = new ST(templateKernelHash);
		int argIndex = 0;
		String tileVarBuffer = getTileBufferName();
		String pointerVarBuffer = commonDefinitions.getDataVarName() + "Ptr->"
				+ getOperationBufferName(operation.variable);
		stKernelHashTile.add("operationName", operationTileName);
		stKernelHashTile.addAggr("setArgs.{index, name}", argIndex++,
				pointerVarBuffer);
		stKernelHashTile.addAggr("setArgs.{index, name}", argIndex++,
				tileVarBuffer);
		String tileSizeVar = isImage ? commonDefinitions.getPrefix()
				+ "tileElemSize" : commonDefinitions.getPrefix() + "tileSize";
		if (isImage) {
			stKernelHashTile.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerWidth());
			stKernelHashTile.add("workSize", getDataPointerHeight());
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
		String destVarBuffer = commonDefinitions.getPrefix() + destVarName
				+ "Buffer";
		stKernelHash.add("operationName",
				commonDefinitions.getOperationName(operation));
		stKernelHash
				.addAggr("setArgs.{index, name}", argIndex++, destVarBuffer);
		if (isImage) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					tileVarBuffer);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerHeight());
		} else {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					pointerVarBuffer);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					tileVarBuffer);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerLength());
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
			expression2 = String.format("%s * %sPtr->height", tileSizeVar,
					commonDefinitions.getDataVarName());
			expression3 = tileSizeVar;
		} else {
			expression = String.format("floor(sqrt((float)%s))",
					getDataPointerLength());
			expression2 = String.format("sizeof(%s) * %s", returnType,
					tileSizeVar);
			expression3 = String.format("sizeof(%s)", returnType);
		}
		st.addAggr("tileData.{type, name, expression}", returnType,
				tileSizeVar, expression);
		st.addAggr("buffers.{bufferName, expression}", tileVarBuffer,
				expression2);
		st.addAggr("destinationVariable.{bufferName, name, type, expression}",
				destVarBuffer, destVarName, returnType, expression3);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
		st.addAggr("task.{operationName}",
				commonDefinitions.getOperationName(operation));
		return st.render();
	}

	protected String createFilter(Operation operation) {
		ST st = new ST(templateFilter);
		st.add("bufferName", getTileBufferName());
		st.add("taskName", getTaskName() + "2");
		st.add("retVar", commonDefinitions.getDataReturnVarName() + "Ptr");
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		st.add("isSequential", isSequential ? "" : null);
		ST stKernelHash = new ST(templateKernelHash);
		int argIndex = 0;
		String pointerRetVarBuffer = commonDefinitions.getDataReturnVarName()
				+ "Ptr->buffer";
		String pointerVarBuffer = commonDefinitions.getDataVarName() + "Ptr->"
				+ getOperationBufferName(operation.variable);
		String operationName = commonDefinitions.getOperationName(operation);
		stKernelHash.add("operationName", operationName);
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				pointerRetVarBuffer);
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				pointerVarBuffer);
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				getTileBufferName());
		if (commonDefinitions.isImage(operation.variable)) {
			st.add("tileSize", String.format("%s * %s", getDataPointerWidth(),
					getDataPointerHeight()));
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerWidth());
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerHeight());
		} else {
			st.add("tileSize", getDataPointerLength());
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerLength());
		}
		stKernelHash.add("workSize", "1");
		st.addAggr("task.{operationName}", operationName);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
		setExternalVariables(st, operation, argIndex);
		String tileSection = createFilterTile(operation);
		return tileSection + st.render();
	}

	private String createFilterTile(Operation operation) {
		boolean isSequential = operation.getExecutionType() == ExecutionType.Sequential;
		ST st = isSequential ? initializeSequentialOperationBody(operation)
				: initializeParallelOperationBody(operation);
		ST stKernelHash = new ST(templateKernelHash);
		int argIndex = 0;
		String tileVarBuffer = getTileBufferName();
		String pointerVarBuffer = commonDefinitions.getDataVarName() + "Ptr->"
				+ getOperationBufferName(operation.variable);
		String operationTileName = commonDefinitions
				.getOperationTileFunctionName(operation);
		stKernelHash.add("operationName", operationTileName);
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				pointerVarBuffer);
		stKernelHash
				.addAggr("setArgs.{index, name}", argIndex++, tileVarBuffer);
		String bufferExpression;
		if (commonDefinitions.isImage(operation.variable)) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerWidth());
			bufferExpression = String.format("sizeof(int) * %s * %s",
					getDataPointerWidth(), getDataPointerHeight());
			if (isSequential) {
				stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
						getDataPointerHeight());
				stKernelHash.add("workSize", "1");
			} else {
				stKernelHash.add("workSize", getDataPointerWidth() + ", "
						+ getDataPointerHeight());
			}
		} else {
			bufferExpression = String.format("sizeof(int) * %s",
					getDataPointerLength());
			if (isSequential) {
				stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
						getDataPointerLength());
				stKernelHash.add("workSize", "1");
			} else {
				stKernelHash.add("workSize", getDataPointerLength());
			}
		}
		setExternalVariables(stKernelHash, operation, argIndex);
		if (isSequential) {
			setBuffers(st, operation);
		}
		st.addAggr("buffers.{bufferName, expression, setArrayBuffer}",
				tileVarBuffer, bufferExpression, null);
		st.addAggr("task.{operationName}", operationTileName);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
		return st.render();
	}

	private ST initializeParallelOperationBody(Operation operation) {
		ST st = new ST(templateParallelOperationBody);
		st.add("returnPointer", null);
		st.add("objectType", getObjectType(operation));
		st.add("tileData", null);
		st.add("destinationVariable", null);
		st.add("buffers", null);
		st.add("taskName", getTaskName());
		return st;
	}

	private ST initializeSequentialOperationBody(Operation operation) {
		ST st = new ST(templateSequentialOperationBody);
		st.add("returnPointer", null);
		st.add("objectType", getObjectType(operation));
		st.add("tileData", null);
		st.add("destinationVariable", null);
		st.add("buffers", null);
		st.add("taskName", getTaskName());
		return st;
	}

	private void setExternalVariables(ST st, Operation operation, int argIndex) {
		for (Variable variable : operation.getExternalVariables()) {
			if (variable.isFinal()) {
				st.addAggr("setArgs.{index, name}", argIndex++, variable.name);
			} else {
				st.addAggr("setArgs.{index, name}", argIndex++,
						commonDefinitions.getPrefix() + variable.name
								+ "Buffer");
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
		String function;
		if (operation.operationType == OperationType.Filter) {
			function = createFilter(operation);
		} else {
			function = createSequentialForeachReduceMap(operation);
		}
		return createOperationSignature(operation, cClassName, true) + " {\n"
				+ function + "}";
	}

	private String createSequentialForeachReduceMap(Operation operation) {
		ST st = initializeSequentialOperationBody(operation);
		String operationName = commonDefinitions.getOperationName(operation);
		st.addAggr("task.{operationName}", operationName);
		ST stKernelHash = new ST(templateKernelHash);
		stKernelHash.add("workSize", "1");
		stKernelHash.add("operationName", operationName);
		stKernelHash.add("setArgs", null);
		st.add("buffers", null);
		int argIndex = 0;
		if (operation.operationType == OperationType.Reduce) {
			String destVarName = operation.destinationVariable.name;
			String type = getJNIType(operation);
			String bufferName = commonDefinitions.getPrefix() + destVarName
					+ "Buffer";
			String expression = String.format(
					"sizeof(%s) * env->GetArrayLength(%s)", type, destVarName);
			st.addAggr("destinationVariable.{bufferName, name, expression}",
					bufferName, destVarName, expression);
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					bufferName);
		} else if (operation.operationType == OperationType.Map) {
			st.addAggr("returnPointer.{objectType, name}", "ArrayData",
					commonDefinitions.getDataReturnVarName());
			stKernelHash
					.addAggr(
							"setArgs.{index, name}",
							argIndex++,
							commonDefinitions.getDataReturnVarName()
									+ "Ptr->"
									+ getOperationBufferName(operation.destinationVariable));
		}
		stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
				commonDefinitions.getDataVarName() + "Ptr->"
						+ getOperationBufferName(operation.variable));
		if (commonDefinitions.isImage(operation.variable)) {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerWidth());
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerHeight());
		} else {
			stKernelHash.addAggr("setArgs.{index, name}", argIndex++,
					getDataPointerLength());
		}
		setExternalVariables(stKernelHash, operation, argIndex);
		setBuffers(st, operation);
		st.addAggr("kernelHash.{body}", stKernelHash.render());
		return st.render();
	}

	private void setBuffers(ST st, Operation operation) {
		for (Variable variable : operation.getExternalVariables()) {
			if (!variable.isFinal()) {
				String prefixedVarName = commonDefinitions.getPrefix()
						+ variable.name;
				String bufferName = prefixedVarName + "Buffer";
				st.addAggr(
						"buffers.{bufferName, expression, arrName, setArrayBuffer}",
						bufferName, String.format("sizeof(%s)",
								commonDefinitions
										.translateToCType(variable.typeName)),
						prefixedVarName, "");
			}
		}
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
					createOperationSignature(operation, cClassName, false));
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
		st.add("varName", declareVarNames ? "" : null);
		if (operation.operationType == OperationType.Reduce) {
			String typeName = getJNIType(operation);
			String decl = String.format("j%sArray", typeName);
			if (declareVarNames)
				decl += " " + operation.destinationVariable.name;
			st.addAggr("params.{decl}", decl);
			st.add("returnType", "void");
		} else if (operation.operationType == OperationType.Map) {
			String decl = "jlong";
			if (declareVarNames)
				decl += " " + commonDefinitions.getDataReturnVarName();
			st.addAggr("params.{decl}", decl);
			st.add("returnType", "void");
		} else if (operation.operationType == OperationType.Filter) {
			st.add("returnType", "jlong");
		} else {
			st.add("returnType", "void");
		}
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
			if (operation.operationType == OperationType.Map) {
				type = commonDefinitions
						.translateToCType(operation.destinationVariable.typeParameters
								.get(0));
			} else {
				type = commonDefinitions
						.translateToCType(operation.destinationVariable.typeName);
			}
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

	private String getOperationBufferName(Variable variable) {
		String bufferName;
		if (commonDefinitions.isImage(variable)) {
			bufferName = "outputBuffer";
		} else {
			bufferName = "buffer";
		}
		return bufferName;
	}

	private String getDataPointerWidth() {
		return commonDefinitions.getDataVarName() + "Ptr->width";
	}

	private String getDataPointerHeight() {
		return commonDefinitions.getDataVarName() + "Ptr->height";
	}

	private String getDataPointerLength() {
		return commonDefinitions.getDataVarName() + "Ptr->length";
	}

	private String getTaskName() {
		return commonDefinitions.getPrefix() + "task";
	}

	private String getTileBufferName() {
		return commonDefinitions.getPrefix() + "tileBuffer";
	}
}
