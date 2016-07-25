/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.RuntimeDefinition.TargetRuntime;
import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.translation.BoxedTypes;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.userlibrary.UserLibraryClass;
import org.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import org.parallelme.compiler.userlibrary.UserLibraryCollection;
import org.parallelme.compiler.userlibrary.UserLibraryDataType;
import org.parallelme.compiler.userlibrary.classes.Array;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.Float32;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.parallelme.compiler.userlibrary.classes.Int16;
import org.parallelme.compiler.userlibrary.classes.Int32;
import org.parallelme.compiler.userlibrary.classes.Pixel;
import org.stringtemplate.v4.ST;

/**
 * Code useful for common runtime definitions.
 * 
 * @author Wilson de Carvalho
 */
public class RuntimeCommonDefinitions {
	private static RuntimeCommonDefinitions instance = new RuntimeCommonDefinitions();
	private final String templateMethodSignature = "<modifier:{var|<var.value> }><returnType> <name>(<params:{var|<var.type> <var.name>}; separator=\", \">)";
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
	 * Return a unique prefixed operation name based on its sequential number.
	 */
	public String getPrefixedOperationName(Operation operation) {
		return this.prefix + this.getOperationName(operation);
	}

	/**
	 * Return a unique operation name based on its sequential number.
	 */
	public String getOperationName(Operation operation) {
		String operationName;
		if (operation.operationType == OperationType.Foreach) {
			operationName = UserLibraryCollection.getForeachMethodName()
					+ operation.sequentialNumber;
		} else if (operation.operationType == OperationType.Reduce) {
			operationName = UserLibraryCollection.getReduceMethodName()
					+ operation.sequentialNumber;
		} else if (operation.operationType == OperationType.Map) {
			operationName = UserLibraryCollection.getMapMethodName()
					+ operation.sequentialNumber;
		} else if (operation.operationType == OperationType.Filter) {
			operationName = UserLibraryCollection.getFilterMethodName()
					+ operation.sequentialNumber;
		} else {
			throw new RuntimeException("Operation not supported: "
					+ operation.operationType);
		}
		return operationName;
	}

	/**
	 * Return a unique name for operation tile function based on its sequential
	 * number.
	 */
	public String getOperationTileFunctionName(Operation operation) {
		return this.getOperationName(operation) + "_tile";
	}

	/**
	 * Return a unique name for operation user function base on its sequential
	 * number.
	 */
	public String getOperationUserFunctionName(Operation operation) {
		return this.getOperationName(operation) + "_func";
	}

	/**
	 * Return a unique name for operation allocation function based on its
	 * sequential number.
	 */
	public String getOperationAllocationFunctionName(Operation operation) {
		return this.getOperationName(operation) + "_setAllocationSize";
	}

	/**
	 * Return a unique method call name based on its sequential number.
	 */
	public String getMethodCallName(MethodCall methodCall) {
		return methodCall.methodName + methodCall.sequentialNumber;
	}

	/**
	 * Return a unique input bind name based on its sequential number.
	 */
	public String getPrefixedInputBindName(InputBind inputBind) {
		return this.prefix + this.getInputBindName(inputBind);
	}

	/**
	 * Return a unique input bind name based on its sequential number.
	 */
	public String getInputBindName(InputBind inputBind) {
		return inputBindName + inputBind.sequentialNumber;
	}

	/**
	 * Return a unique prefixed output bind name based on its sequential number.
	 */
	public String getPrefixedOutputBindName(OutputBind outputBind) {
		return this.prefix + this.getOutputBindName(outputBind);
	}

	/**
	 * Return a unique output bind name based on its sequential number.
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
	public <T> String toCommaSeparatedString(List<T> parameters) {
		StringBuilder params = new StringBuilder();
		for (int i = 0; i < parameters.size(); i++) {
			params.append(parameters.get(i));
			if (i != (parameters.size() - 1))
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
	public <T extends Parameter> String createJavaMethodSignature(
			String modifier, String returnType, String name,
			List<T> parameters, boolean asArrayVariables) {
		ST st = new ST(templateMethodSignature);
		if (modifier.isEmpty())
			st.add("modifier", null);
		else
			st.addAggr("modifier.{value}", modifier);
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
	 * 
	 * @param inputBind
	 *            Input bind that will have a signature method created.
	 * @param isInterface
	 *            True if the signature is being created for a java interface,
	 *            false for a class.
	 */
	public String createJavaMethodSignature(InputBind inputBind,
			boolean isInterface) throws CompilationException {
		String modifier = isInterface ? "" : "public";
		return this.createJavaMethodSignature(modifier, "void",
				this.getInputBindName(inputBind),
				this.createJavaImplParameters(inputBind), false);
	}

	/**
	 * Creates a Java default list of parameters for a given input bind to be
	 * used for method declaration in Java implementation classes.
	 */
	public List<Parameter> createJavaImplParameters(InputBind inputBind)
			throws CompilationException {
		List<Parameter> parameters = inputBind.parameters;
		if (inputBind.variable.typeName.equals(Array.getInstance()
				.getClassName())) {
			if (inputBind.parameters.size() != 2)
				throw new CompilationException(
						"Array constructor must have 2 arguments: primitive type array and NumericalData class.");
			// Second element (NumericalData class) in original parameters is
			// not used
			parameters = new ArrayList<>();
			parameters.add(inputBind.parameters.get(0));
		} else if (inputBind.variable.typeName.equals(HDRImage.getInstance()
				.getClassName())) {
			if (inputBind.parameters.size() != 3)
				throw new CompilationException(
						"HDRImage constructor must have 3 arguments: byte array, width and height.");
			parameters = new ArrayList<>();
			parameters.add(new Variable("data", "byte[]", null, "", -1));
			parameters.add(new Variable("width", "int", null, "", -1));
			parameters.add(new Variable("height", "int", null, "", -1));
		}
		return parameters;
	}

	/**
	 * Creates a Java method signature for a given operation.
	 * 
	 * @param operation
	 *            Operation that will have a signature method created.
	 * @param isInterface
	 *            True if the signature is being created for a java interface,
	 *            false for a class.
	 */
	public String createJavaMethodSignature(Operation operation,
			boolean isInterface) {
		String returnType = operation.destinationVariable == null
				|| operation.destinationVariable.typeName.equals(Array
						.getInstance().getClassName()) ? "void"
				: operation.destinationVariable.typeName;
		String modifier = isInterface ? "" : "public";
		if (operation.getExecutionType() == ExecutionType.Sequential) {
			return this.createJavaMethodSignature(modifier, returnType,
					this.getOperationName(operation),
					operation.getExternalVariables(), true);
		} else {
			return this.createJavaMethodSignature(modifier, returnType,
					this.getOperationName(operation),
					operation.getExternalVariables(), false);
		}
	}

	/**
	 * Creates a Java method signature for a given output bind.
	 * 
	 * @param outputBind
	 *            Output bind that will have a signature method created.
	 * @param isInterface
	 *            True if the signature is being created for a java interface,
	 *            false for a class.
	 */
	public String createJavaMethodSignature(OutputBind outputBind,
			boolean isInterface) {
		String modifier = isInterface ? "" : "public";
		String ret;
		if (outputBind.outputBindType == OutputBindType.None) {
			ArrayList<Variable> parameters = new ArrayList<>();
			parameters.add(outputBind.destinationObject);
			ret = this.createJavaMethodSignature(modifier, "void",
					this.getOutputBindName(outputBind), parameters, false);
		} else {
			ret = this.createJavaMethodSignature(modifier,
					outputBind.destinationObject.typeName,
					this.getOutputBindName(outputBind),
					new ArrayList<Variable>(), false);
		}
		return ret;
	}

	/**
	 * Creates a Java method signature for a given method call.
	 * 
	 * @param methodCall
	 *            Method call that will have a signature method created.
	 * @param isInterface
	 *            True if the signature is being created for a java interface,
	 *            false for a class.
	 */
	public String createJavaMethodSignature(MethodCall methodCall,
			boolean isInterface) {
		UserLibraryClass userLibrary = UserLibraryClassFactory
				.getClass(methodCall.variable.typeName);
		String modifier = isInterface ? "" : "public";
		return this.createJavaMethodSignature(modifier,
				userLibrary.getReturnType(methodCall.methodName),
				this.getMethodCallName(methodCall), new ArrayList<Variable>(),
				isInterface);
	}

	/**
	 * Translates a given type to an equivalent C type. Example: translate RGB
	 * type to float3 in RenderScript or ParallelME runtime.
	 * 
	 * @param typeName
	 *            Type that must be translated.
	 * @return A string with the equivalent type for RenderScript and ParallelME
	 *         runtimes.
	 */
	public String translateToCType(String typeName) {
		String translatedType = "";
		if (typeName.equals(Pixel.getInstance().getClassName())) {
			translatedType = "float4";
		} else if (typeName.equals(HDRImage.getInstance().getClassName())) {
			translatedType = "float";
		} else if (typeName.equals(BitmapImage.getInstance().getClassName())) {
			translatedType = "float";
		} else if (typeName.equals(Int16.getInstance().getClassName())) {
			translatedType = "short";
		} else if (typeName.equals(Int32.getInstance().getClassName())) {
			translatedType = "int";
		} else if (typeName.equals(Float32.getInstance().getClassName())) {
			translatedType = "float";
		} else if (PrimitiveTypes.isPrimitive(typeName)) {
			translatedType = PrimitiveTypes.getCType(typeName);
		} else if (BoxedTypes.isBoxed(typeName)) {
			translatedType = BoxedTypes.getCType(typeName);
		}
		return translatedType;
	}

	/**
	 * Returns an equivalent Java type for a given user-library data type.
	 */
	public String translateToJavaType(String typeName) {
		UserLibraryClass userLibraryClass = UserLibraryClassFactory
				.getClass(typeName);
		if (userLibraryClass != null) {
			if (!(userLibraryClass instanceof UserLibraryDataType)) {
				throw new RuntimeException(String.format(
						"'%s' is not convertible to a Java primitive type.",
						typeName));
			}
			return ((UserLibraryDataType) userLibraryClass)
					.getJavaPrimitiveType();
		} else {
			throw new RuntimeException(String.format(
					"'%s' is not a valid primitiveUser Library "
							+ "Data Type convertible to a Java primitive type.",
					typeName));
		}
	}

	public String removeCurlyBraces(String code) {
		// Remove the last curly brace
		code = code.substring(0, code.lastIndexOf("}"));
		// Remove the first curly brace
		code = code.substring(code.indexOf("{") + 1, code.length());
		return code;
	}

	/**
	 * Checks if a given variable has an image type.
	 */
	public boolean isImage(Variable variable) {
		return variable.typeName.equals(BitmapImage.getInstance()
				.getClassName())
				|| variable.typeName.equals(HDRImage.getInstance()
						.getClassName());
	}
}
