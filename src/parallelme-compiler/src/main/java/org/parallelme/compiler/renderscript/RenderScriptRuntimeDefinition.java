/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.renderscript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.stringtemplate.v4.ST;
import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.RuntimeDefinitionImpl;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.userlibrary.classes.*;
import org.parallelme.compiler.util.FileWriter;

/**
 * General definitions for RenderScript runtime.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class RenderScriptRuntimeDefinition extends RuntimeDefinitionImpl {
	private static final String templateRSFile = "<introductoryMsg>\n\n<header>\n<functions:{functionName|\n\n<functionName>}>";
	private static final String templateKernels = "private ScriptC_<originalClassName> <kernelName>;\n\n";
	private static final String templateConstructor = "public <javaClassName>(RenderScript PM_mRS) {\n\tthis.PM_mRS = PM_mRS;\n"
			+ "\tthis.<kernelName> = new ScriptC_<originalClassName>(PM_mRS);\n\\}\n";

	public RenderScriptRuntimeDefinition(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		super(cCodeTranslator, outputDestinationFolder);
		this.initTranslators();
	}

	private void initTranslators() {
		if (super.translators == null) {
			super.translators = new HashMap<>();
			super.translators.put(Array.getName(), new RSArrayTranslator(
					cCodeTranslator));
			super.translators.put(BitmapImage.getName(),
					new RSBitmapImageTranslator(cCodeTranslator));
			super.translators.put(HDRImage.getName(), new RSHDRImageTranslator(
					cCodeTranslator));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TargetRuntime getTargetRuntime() {
		return TargetRuntime.RenderScript;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getIsValidBody() {
		ArrayList<String> ret = new ArrayList<>();
		ret.add("return true;");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getInitializationString(String className,
			OperationsAndBinds operationsAndBinds, List<MethodCall> methodCalls) {
		StringBuilder init = new StringBuilder();
		init.append("private RenderScript PM_mRS;\n");
		ST st1 = new ST(templateKernels);
		ST st2 = new ST(templateConstructor);
		String javaClassName = RuntimeCommonDefinitions.getInstance()
				.getJavaWrapperClassName(className, this.getTargetRuntime());
		st1.add("originalClassName", className);
		st1.add("kernelName", RuntimeCommonDefinitions.getInstance()
				.getKernelName(className));
		st2.add("javaClassName", javaClassName);
		st2.add("originalClassName", className);
		st2.add("kernelName", RuntimeCommonDefinitions.getInstance()
				.getKernelName(className));
		init.append(st1.render());
		init.append(st2.render());
		ArrayList<String> ret = new ArrayList<String>();
		String[] tmp = init.toString().split("\n");
		for (int i = 0; i < tmp.length; i++)
			ret.add(tmp[i]);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getImports() {
		ArrayList<String> ret = new ArrayList<>();
		ret.add("android.support.v8.renderscript.*");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void translateOperationsAndBinds(String packageName,
			String className, OperationsAndBinds operationsAndBinds) {
		// 1. Add file header
		ST st = new ST(templateRSFile);
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		st.add("header", "#pragma version(1)\n#pragma rs java_package_name("
				+ packageName + ")");
		// 2. Translate input binds
		Set<String> inputBindTypes = new HashSet<String>();
		for (InputBind inputBind : operationsAndBinds.inputBinds) {
			if (!inputBindTypes.contains(inputBind.variable.typeName)) {
				inputBindTypes.add(inputBind.variable.typeName);
				st.add("functions",
						this.translators.get(inputBind.variable.typeName)
								.translateInputBind(className, inputBind));
			}
		}
		// 3. Translate operations
		for (Operation operation : operationsAndBinds.operations) {
			if (operation.operationType == OperationType.Foreach) {
				st.add("functions",
						this.translators.get(operation.variable.typeName)
								.translateForeach(className, operation));
			}
		}
		// 4. Translate outputbinds
		Set<String> outputBindTypes = new HashSet<String>();
		for (OutputBind outputBind : operationsAndBinds.outputBinds) {
			if (!outputBindTypes.contains(outputBind.variable.typeName)) {
				outputBindTypes.add(outputBind.variable.typeName);
				st.add("functions",
						this.translators.get(outputBind.variable.typeName)
								.translateOutputBind(className, outputBind));
			}
		}
		// 5. Write translated file
		FileWriter
				.writeFile(
						className + ".rs",
						RuntimeCommonDefinitions.getInstance()
								.getRSDestinationFolder(
										this.outputDestinationFolder,
										packageName), st.render());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exportInternalLibrary(String packageName,
			String destinationFolder) throws IOException {
	}
}
