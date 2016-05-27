/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.io.File;

import org.parallelme.compiler.RuntimeDefinition.TargetRuntime;
import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.userlibrary.UserLibraryClass;
import org.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import org.parallelme.compiler.userlibrary.UserLibraryCollectionClass;
import org.parallelme.compiler.userlibrary.classes.Array;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Code useful for common runtime definitions.
 * 
 * @author Wilson de Carvalho
 */
public class RuntimeCommonDefinitions {
	private static RuntimeCommonDefinitions instance = new RuntimeCommonDefinitions();
	private final String templateMethodSignature = "<modifier> <returnType> <name>(<params:{var|<var.type> <var.name>}; separator=\", \">)";
	private final String inSuffix = "In";
	private final String outSuffix = "Out";
	private final String inputBindName = "inputBind";
	private final String outputBindName = "outputBind";
	private final String prefix = "PM_";
	private final String headerComment = "**                                               _    __ ____\n"
			+ " *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/\n"
			+ " *  |  _ \\/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__\n"
			+ " *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__\n"
			+ " *  |_| /_/ |_|_|\\_\\/_/ |_/____/___/___/____/ /_/  /_/____/\n"
			+ " *\n"
			+ " * Code created automatically by ParallelME compiler.\n" + " *";

	private RuntimeCommonDefinitions() {
	}

	public static RuntimeCommonDefinitions getInstance() {
		return instance;
	}

	public String getVariableInName(Variable variable) {
		return prefix + variable.name + variable.sequentialNumber + inSuffix;
	}

	public String getVariableOutName(Variable variable) {
		return prefix + variable.name + variable.sequentialNumber + outSuffix;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getHeaderComment() {
		return "/" + this.headerComment + "/";
	}

	public String getAndroidMKHeaderComment() {
		return "#" + this.headerComment.replaceAll("\\*", "#") + "#";
	}

	/**
	 * Return an unique prefixed operation name base on its sequential number.
	 */
	public String getPrefixedOperationName(Operation operation) {
		return this.prefix + this.getOperationName(operation);
	}

	/**
	 * Return an unique operation name base on its sequential number.
	 */
	public String getOperationName(Operation operation) {
		if (operation.operationType == OperationType.Foreach) {
			return UserLibraryCollectionClass.getForeachMethodName()
					+ operation.sequentialNumber;
		} else {
			return UserLibraryCollectionClass.getReduceMethodName()
					+ operation.sequentialNumber;
		}
	}

	/**
	 * Return an unique method call name base on its sequential number.
	 */
	public String getMethodCallName(MethodCall methodCall) {
		return methodCall.methodName + methodCall.sequentialNumber;
	}

	/**
	 * Return an unique input bind name base on its sequential number.
	 */
	public String getPrefixedInputBindName(InputBind inputBind) {
		return this.prefix + this.getInputBindName(inputBind);
	}

	/**
	 * Return an unique input bind name base on its sequential number.
	 */
	public String getInputBindName(InputBind inputBind) {
		return inputBindName + inputBind.sequentialNumber;
	}

	/**
	 * Return an unique prefixed output bind name base on its sequential number.
	 */
	public String getPrefixedOutputBindName(OutputBind outputBind) {
		return this.prefix + this.getOutputBindName(outputBind);
	}

	/**
	 * Return an unique output bind name base on its sequential number.
	 */
	public String getOutputBindName(OutputBind outputBind) {
		return outputBindName + outputBind.sequentialNumber;
	}

	/**
	 * Return kernel that must be used in kernel object declarations.
	 */
	public String getKernelName(String className) {
		return this.prefix + "kernel";
	}

	/**
	 * Transforms an array of parameters in a comma separated string.
	 * 
	 * @param parameters
	 *            Array of parameters.
	 * @return Comma separated string with parameters.
	 */
	public String toCommaSeparatedString(Parameter[] parameters) {
		StringBuilder params = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			params.append(parameters[i]);
			if (i != (parameters.length - 1))
				params.append(", ");
		}
		return params.toString();
	}

	/**
	 * Gets a pointer name for a given variable.
	 */
	public String getPointerName(Variable variable) {
		return this.getPrefix() + variable.name + variable.sequentialNumber
				+ "Ptr";
	}

	/**
	 * Gets the runtime pointer name.
	 */
	public String getRuntimePointerName() {
		return this.getPrefix() + "runtimePtr";
	}

	/**
	 * Return a standard C class name based on original Java package and class
	 * names.
	 */
	public String getCClassName(String packageName, String className) {
		return packageName.replaceAll("\\.", "_") + "_" + className;
	}

	/**
	 * Return a destination folder for output files in Java.
	 */
	public String getJavaDestinationFolder(String baseDestinationFolder,
			String packageName) {
		return baseDestinationFolder + File.separator + "java" + File.separator
				+ packageName.replaceAll("\\.", "/") + File.separator;
	}

	/**
	 * Return a destination folder for output JNI files.
	 */
	public String getJNIDestinationFolder(String baseDestinationFolder) {
		return baseDestinationFolder + File.separator + "jni" + File.separator
				+ "ParallelME" + File.separator + "generated" + File.separator;
	}

	/**
	 * Return a destination folder for output RenderScript files.
	 */
	public String getRSDestinationFolder(String baseDestinationFolder,
			String packageName) {
		return baseDestinationFolder + File.separator + "rs" + File.separator
				+ packageName.replaceAll("\\.", "/") + File.separator;
	}

	/**
	 * Return the Java wrapper interface name that must be created for a given
	 * class.
	 */
	public String getJavaWrapperInterfaceName(String className) {
		return className + "Wrapper";
	}

	/**
	 * Return a Java wrapper class name for a given target runtime.
	 */
	public String getJavaWrapperClassName(String className,
			TargetRuntime targetRuntime) {
		if (targetRuntime == TargetRuntime.ParallelME) {
			return className + "WrapperImplPM";
		} else {
			return className + "WrapperImplRS";
		}
	}

	/**
	 * Return the name of ParallelME object.
	 */
	public String getParallelMEObjectName() {
		return this.getPrefix() + "parallelME";
	}

	/**
	 * Creates a method's signature that can be used in Java code.
	 * 
	 * @param modifier
	 *            Method's modifier.
	 * @param returnType
	 *            Method's return type.
	 * @param name
	 *            Method's name
	 * @param parameters
	 *            Method's parameters (only Variable and Literal instances are
	 *            accepted).
	 * @param asArrayVariables
	 *            If true, all NON-FINAL variables are declared as arrays in the
	 *            method signature.
	 * 
	 * @throws CompilationException
	 *             Exception thrown in case any of invalid parameter type.
	 */
	public String createJavaMethodSignature(String modifier, String returnType,
			String name, Parameter[] parameters, boolean asArrayVariables) {
		ST st = new ST(templateMethodSignature);
		st.add("modifier", modifier);
		st.add("returnType", returnType);
		st.add("name", name);
		st.add("params", null);
		int i = 0;
		for (Parameter param : parameters) {
			if (param instanceof Variable) {
				Variable variable = (Variable) param;
				if (asArrayVariables && !variable.isFinal()) {
					st.addAggr("params.{type, name}", variable.typeName + "[]",
							variable.name);
				} else {
					st.addAggr("params.{type, name}", variable.typeName,
							variable.name);
				}
			} else if (param instanceof Literal) {
				Literal literal = (Literal) param;
				String literalName = "literal" + i++;
				st.addAggr("params.{type, name}", literal.typeName, literalName);
			} else {
				throw new RuntimeException(
						"Invalid parameter type for method signature creation.");
			}
		}
		return st.render();
	}

	/**
	 * Creates a Java method signature for a given input bind.
	 */
	public String createJavaMethodSignature(InputBind inputBind)
			throws CompilationException {
		return this.createJavaMethodSignature("public", "void",
				this.getInputBindName(inputBind),
				this.createJavaImplParameters(inputBind), false);
	}

	/**
	 * Creates a Java default list of parameters for a given input bind to be
	 * used for method declaration in Java implementation classes.
	 */
	public Parameter[] createJavaImplParameters(InputBind inputBind)
			throws CompilationException {
		Parameter[] parameters = inputBind.parameters;
		if (inputBind.variable.typeName.equals(Array.getName())) {
			if (inputBind.parameters.length != 3)
				throw new CompilationException(
						"Array constructor must have 3 arguments: primitive type array, NumericalData class and arrray length.");
			// Second element (NumericalData class) in original parameters is
			// not used
			parameters = new Parameter[2];
			parameters[0] = inputBind.parameters[0];
			// Checks if the array length parameter is a expression. In case
			// positive, temporarily translates it to a literal integer in order
			// to create a valid signature.
			parameters[1] = inputBind.parameters[2] instanceof Expression ? new Literal(
					"1", "int") : inputBind.parameters[2];
		} else if (inputBind.variable.typeName.equals(HDRImage.getName())) {
			if (inputBind.parameters.length != 3)
				throw new CompilationException(
						"Array constructor must have 3 arguments: byte array, width and height.");
			parameters = new Parameter[3];
			parameters[0] = new Variable("data", "byte[]", "", "", -1);
			parameters[1] = new Variable("width", "int", "", "", -1);
			parameters[2] = new Variable("height", "int", "", "", -1);
		}
		return parameters;
	}

	/**
	 * Creates a Java method signature for a given operation.
	 */
	public String createJavaMethodSignature(Operation operation) {
		if (operation.getExecutionType() == ExecutionType.Sequential) {
			return this.createJavaMethodSignature("public", "void",
					this.getOperationName(operation),
					operation.getExternalVariables(), true);
		} else {
			return this.createJavaMethodSignature("public", "void",
					this.getOperationName(operation),
					operation.getExternalVariables(), false);
		}
	}

	/**
	 * Creates a Java method signature for a given output bind.
	 */
	public String createJavaMethodSignature(OutputBind outputBind) {
		return this.createJavaMethodSignature("public", "void",
				this.getOutputBindName(outputBind),
				new Variable[] { outputBind.destinationObject }, false);
	}

	/**
	 * Creates a Java method signature for a given method call.
	 */
	public String createJavaMethodSignature(MethodCall methodCall) {
		UserLibraryClass userLibrary = UserLibraryClassFactory
				.create(methodCall.variable.typeName);
		return this.createJavaMethodSignature("public",
				userLibrary.getReturnType(methodCall.methodName),
				this.getMethodCallName(methodCall), new Variable[0], false);
	}
}
