/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.userlibrary;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.RuntimeCommonDefinitions;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.intermediate.Operation.ExecutionType;
import org.parallelme.compiler.translation.BoxedTypes;
import org.parallelme.compiler.translation.PrimitiveTypes;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;
import org.parallelme.compiler.userlibrary.classes.Float32;
import org.parallelme.compiler.userlibrary.classes.Int16;
import org.parallelme.compiler.userlibrary.classes.Int32;
import org.parallelme.compiler.userlibrary.classes.Pixel;

/**
 * Base class for translators containing code that is shared between different
 * target runtimes.
 * 
 * @author Wilson de Carvalho
 */
public abstract class BaseUserLibraryTranslator implements
		UserLibraryTranslatorDefinition {
	protected RuntimeCommonDefinitions commonDefinitions = RuntimeCommonDefinitions
			.getInstance();

	/**
	 * Create a global variable name for the given variable following some
	 * standards. Global variables will be prefixed with "g" followed by an
	 * upper case letter and sufixed by the operation name, so "max" from
	 * foreach 2 becomes "gMax_Foreach2"
	 */
	protected String getGlobalVariableName(String variable, Operation operation) {
		String operationName = this.commonDefinitions
				.getOperationName(operation);
		String variableName = this.upperCaseFirstLetter(variable);
		return this.commonDefinitions.getPrefix() + "g" + variableName
				+ this.upperCaseFirstLetter(operationName);
	}

	/**
	 * Change the first letter of the informed string to upper case.
	 */
	protected String upperCaseFirstLetter(String string) {
		return string.substring(0, 1).toUpperCase()
				+ string.substring(1, string.length());
	}

	/**
	 * Translates variables on the give code to a correspondent runtime-specific
	 * type. Example: replaces all RGB objects by float3 on RenderScript.
	 * 
	 * @param variable
	 *            Variable that must be translated.
	 * @param code
	 *            Original code that must have the reference replaced.
	 * @return A string with the new code with the variable replaced.
	 */
	public String translateVariable(Variable variable, String code) {
		String translatedCode = "";
		if (variable.typeName.equals(Pixel.getInstance().getClassName())) {
			translatedCode = this.translatePixelVariable(variable, code);
		} else if (variable.typeName.equals(Int16.getInstance().getClassName())
				|| variable.typeName.equals(Int32.getInstance().getClassName())
				|| variable.typeName.equals(Float32.getInstance()
						.getClassName())) {
			translatedCode = this.translateNumericVariable(variable, code);
		} else if (PrimitiveTypes.isPrimitive(variable.typeName)) {
			translatedCode = code.replaceAll(variable.typeName,
					PrimitiveTypes.getCType(variable.typeName));
		} else if (BoxedTypes.isBoxed(variable.typeName)) {
			translatedCode = code.replaceAll(variable.typeName,
					BoxedTypes.getCType(variable.typeName));
		}
		return translatedCode;
	}

	public String translatePixelVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.commonDefinitions.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".x", "x");
		ret = ret.replaceAll(variable.name + ".y", "y");
		ret = ret
				.replaceAll(variable.name + ".rgba.red", variable.name + ".s0");
		ret = ret.replaceAll(variable.name + ".rgba.green", variable.name
				+ ".s1");
		ret = ret.replaceAll(variable.name + ".rgba.blue", variable.name
				+ ".s2");
		ret = ret.replaceAll(variable.name + ".rgba.alpha", variable.name
				+ ".s3");
		return ret;
	}

	public String translateNumericVariable(Variable variable, String code) {
		String ret = code.replaceAll(variable.typeName,
				this.commonDefinitions.translateType(variable.typeName));
		ret = ret.replaceAll(variable.name + ".value", variable.name);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> translateOperation(Operation operation) {
		List<String> ret = new ArrayList<>();
		// Translate parallel operations
		if (operation.getExecutionType() == ExecutionType.Parallel) {
			if (operation.operationType == OperationType.Foreach) {
				ret.add(this.translateParallelForeach(operation));
			} else if (operation.operationType == OperationType.Reduce) {
				// C functions must be declared before used, so user function
				// must be the first
				ret.add(this.translateParallelReduceUserFunction(operation));
				ret.add(this.translateParallelReduceTile(operation));
				ret.add(this.translateParallelReduce(operation));
			} else {
				throw new RuntimeException("Invalid operation: "
						+ operation.operationType);
			}
		} else {
			if (operation.operationType == OperationType.Foreach) {
				ret.add(this.translateSequentialForeach(operation));
			} else if (operation.operationType == OperationType.Reduce) {
				ret.add(this.translateSequentialReduce(operation));
			} else {
				throw new RuntimeException("Invalid operation: "
						+ operation.operationType);
			}
		}
		return ret;
	}

	/**
	 * Translates a parallel operation returning a C code compatible with this
	 * runtime.
	 * 
	 * @param operation
	 *            Operation that must be translated.
	 * @return C code with operation's user code compatible with this runtime.
	 */
	abstract protected String translateParallelForeach(Operation operation);

	/**
	 * Translates a tile operation returning a C code compatible with this
	 * runtime.
	 * 
	 * @param operation
	 *            Operation that must be translated.
	 * @return C code with operation's user code compatible with this runtime.
	 */
	abstract protected String translateParallelReduce(Operation operation);

	/**
	 * Translates a tile operation returning a C code compatible with this
	 * runtime.
	 * 
	 * @param operation
	 *            Operation that must be translated.
	 * @return C code with operation's user code compatible with this runtime.
	 */
	abstract protected String translateParallelReduceTile(Operation operation);

	/**
	 * Translates the user code that will be used for composing parallel and
	 * tile operations.
	 * 
	 * @param operation
	 *            Operation that must be translated.
	 * @return C code with operation's user code compatible with this runtime.
	 */
	abstract protected String translateParallelReduceUserFunction(
			Operation operation);

	/**
	 * Translates a sequential operation returning a C code compatible with this
	 * runtime.
	 * 
	 * @param operation
	 *            Operation that must be translated.
	 * @return C code with operation's user code compatible with this runtime.
	 */
	abstract protected String translateSequentialForeach(Operation operation);

	/**
	 * Translates a sequential operation returning a C code compatible with this
	 * runtime.
	 * 
	 * @param operation
	 *            Operation that must be translated.
	 * @return C code with operation's user code compatible with this runtime.
	 */
	abstract protected String translateSequentialReduce(Operation operation);
}
