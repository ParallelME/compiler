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

import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.RuntimeDefinitionImpl;
import org.parallelme.compiler.SimpleLogger;
import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.userlibrary.classes.*;
import org.stringtemplate.v4.ST;

/**
 * General definitions for ParallelME runtime.
 * 
 * @author Wilson de Carvalho
 */
public class ParallelMERuntimeDefinition extends RuntimeDefinitionImpl {
	public ParallelMERuntimeDefinition(CTranslator cCodeTranslator,
			String outputDestinationFolder) {
		super(cCodeTranslator, outputDestinationFolder);
		this.initTranslators();
	}

	private void initTranslators() {
		if (super.translators == null) {
			super.translators = new HashMap<>();
			super.translators.put(HDRImage.getName(), new PMHDRImageTranslator(
					cCodeTranslator));
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
		ret.add("return ParallelMERuntime.getInstance().runtimePointer != 0;");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getInitializationString(String className,
			IteratorsAndBinds iteratorsAndBinds, List<MethodCall> methodCalls)
			throws CompilationException {
		List<String> ret = new ArrayList<>();
		ret.addAll(this.declarePointers(iteratorsAndBinds.inputBinds));
		// Declare native functions to user JNI
		ret.add(" ");
		ret.addAll(this.declareNativeIterators(iteratorsAndBinds.iterators));
		ret.add(" ");
		ret.addAll(this.cleanUpPointers(iteratorsAndBinds.inputBinds));
		ret.add(" ");
		ret.addAll(this.initializeParallelME());
		return ret;
	}

	private List<String> declarePointers(List<InputBind> inputBinds)
			throws CompilationException {
		List<String> ret = new ArrayList<>();
		Set<Variable> variables = new HashSet<>();
		// Store variables in a set to avoid duplication if the user creates two
		// input binds for the same variable
		for (InputBind inputBind : inputBinds)
			variables.add(inputBind.variable);
		if (!variables.isEmpty()) {
			ST st = new ST(
					"private long <pointer:{var|<var.name>}; separator=\", \">;");
			for (Variable variable : variables) {
				st.addAggr("pointer.{name}", RuntimeCommonDefinitions
						.getInstance().getPointerName(variable));
			}
			ret.add(st.render());
		}
		return ret;
	}

	private List<String> declareNativeIterators(List<Iterator> iterators)
			throws CompilationException {
		List<String> ret = new ArrayList<>();
		Variable runtimePtr = new Variable("runtimePtr", "long", "", "", -1);
		Variable variablePointer = new Variable("varPtr", "long", "", "", -1);
		for (Iterator iterator : iterators) {
			Parameter[] parameters;
			// Sequential iterators must create an array for each variable. This
			// array will be used to store the output value.
			Variable[] externalVariables = iterator.getExternalVariables();
			if (iterator.getType() == IteratorType.Sequential) {
				parameters = new Parameter[externalVariables.length * 2 + 2];
				parameters[0] = runtimePtr;
				parameters[1] = variablePointer;
				int j = 0;
				for (int i = 2; i < parameters.length; i += 2) {
					Variable foo = externalVariables[j];
					parameters[i] = foo;
					parameters[i + 1] = new Variable(RuntimeCommonDefinitions
							.getInstance().getPrefix() + foo.name, foo.typeName
							+ "[]", "", "", -1);
					j++;
				}
			} else {
				parameters = new Parameter[externalVariables.length + 2];
				parameters[0] = runtimePtr;
				parameters[1] = variablePointer;
				System.arraycopy(externalVariables, 0, parameters, 2,
						externalVariables.length);
			}
			String name = RuntimeCommonDefinitions.getInstance()
					.getIteratorName(iterator);
			ret.add(RuntimeCommonDefinitions.getInstance()
					.createJavaMethodSignature("private native", "void", name,
							parameters, false)
					+ ";");
		}
		return ret;
	}

	private List<String> cleanUpPointers(List<InputBind> inputBinds) {
		ArrayList<String> ret = new ArrayList<>();
		Set<Variable> variables = new HashSet<>();
		for (InputBind inputBind : inputBinds)
			variables.add(inputBind.variable);
		ret.add("@Override");
		ret.add("protected void finalize() throws Throwable {");
		for (Variable variable : variables) {
			if (variable.typeName.equals(HDRImage.getName())
					|| variable.typeName.equals(BitmapImage.getName()))
				ret.add(String.format(
						"\tParallelMERuntime.getInstance().cleanUpImage(%s);",
						RuntimeCommonDefinitions.getInstance().getPointerName(
								variable)));
			else if (variable.typeName.equals(Array.getName()))
				ret.add(String.format(
						"\tParallelMERuntime.getInstance().cleanUpArray(%s);",
						RuntimeCommonDefinitions.getInstance().getPointerName(
								variable)));
		}
		ret.add("\tsuper.finalize();");
		ret.add("}");
		ret.add("");
		return ret;
	}

	private List<String> initializeParallelME() {
		ArrayList<String> ret = new ArrayList<>();
		ret.add("static {");
		ret.add("\tSystem.loadLibrary(\"ParallelMEGenerated\");");
		ret.add("}");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getImports() {
		ArrayList<String> ret = new ArrayList<>();
		ret.add("org.parallelme.ParallelMERuntime");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void translateIteratorsAndBinds(String packageName,
			String className, IteratorsAndBinds iteratorsAndBinds) {
		ParallelMERuntimeCTranslation cTranslation = new ParallelMERuntimeCTranslation();
		cTranslation.createKernelFile(className, iteratorsAndBinds,
				this.translators, this.outputDestinationFolder);
		String cClassName = RuntimeCommonDefinitions.getInstance()
				.getJavaWrapperClassName(className, TargetRuntime.ParallelME);
		cTranslation.createCPPFile(packageName, cClassName,
				iteratorsAndBinds.iterators, this.outputDestinationFolder);
		cTranslation.createHFile(packageName, cClassName,
				iteratorsAndBinds.iterators, this.outputDestinationFolder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exportInternalLibrary(String packageName,
			String destinationFolder) throws IOException {
		this.exportResource("ParallelME", destinationFolder);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateMethodCall(MethodCall methodCall) {
		String ret = "";
		if (methodCall.variable.typeName.equals(BitmapImage.getName())
				|| methodCall.variable.typeName.equals(HDRImage.getName())) {
			if (methodCall.methodName.equals(BitmapImage.getInstance()
					.getWidthMethodName())) {
				ret = RuntimeCommonDefinitions.getInstance().getVariableInName(
						methodCall.variable)
						+ ".getType().getX()";
			} else if (methodCall.methodName.equals(BitmapImage.getInstance()
					.getHeightMethodName())) {
				ret = RuntimeCommonDefinitions.getInstance().getVariableInName(
						methodCall.variable)
						+ ".getType().getY()";
			}
		} else {
			SimpleLogger.error("");
		}
		return ret;
	}
}
