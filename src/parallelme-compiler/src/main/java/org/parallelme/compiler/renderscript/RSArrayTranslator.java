/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.renderscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.ArrayTranslator;
import org.parallelme.compiler.userlibrary.classes.Float32;
import org.parallelme.compiler.userlibrary.classes.Int16;
import org.parallelme.compiler.userlibrary.classes.Int32;
import org.stringtemplate.v4.ST;

/**
 * Definitions for Array translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public class RSArrayTranslator extends RSTranslator implements ArrayTranslator {
	private static final String templateCreateAllocation = "<allocation> = Allocation.createSized(PM_mRS, Element.<elementType>(PM_mRS), <inputArray>.length);\n"
			+ "<allocation>.copyFrom(<inputArray>);";
	private static final String templateCreateOutputAllocation = "<name> = (<type>) java.lang.reflect.Array.newInstance(<baseType>.class, <inputAllocation>.getType().getX());\n";
	private static final String templateAllocationCopyTo = "<inputObject>.copyTo(<destinationObject>);";

	// Keeps a key-value map of equivalent types from ParallelME to RenderScript
	// allocation.
	private static Map<String, String> parallelME2RSAllocationTypes = null;

	public RSArrayTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
		if (parallelME2RSAllocationTypes == null)
			this.initParallelME2RSAllocationTypes();
	}

	private void initParallelME2RSAllocationTypes() {
		parallelME2RSAllocationTypes = new HashMap<>();
		parallelME2RSAllocationTypes.put(Int16.getName(), "I16");
		parallelME2RSAllocationTypes.put(Int32.getName(), "I32");
		parallelME2RSAllocationTypes.put(Float32.getName(), "F32");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBind(String className, InputBind inputBind) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjCreation(String className,
			InputBind inputBind) {
		String inputObject = this.commonDefinitions
				.getVariableInName(inputBind.variable);
		ST st = new ST(templateCreateAllocation);
		// TODO Check if parameters array has size 1, otherwise throw an
		// exception and abort translation.
		st.add("inputArray", inputBind.parameters[0]);
		st.add("allocation", inputObject);
		st.add("elementType", parallelME2RSAllocationTypes
				.get(inputBind.variable.typeParameterName));
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjDeclaration(InputBind inputBind) {
		String inAllocation = this.commonDefinitions
				.getVariableInName(inputBind.variable);
		return String.format("private Allocation %s;", inAllocation);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBind(String className, OutputBind outputBind) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBindCall(String className,
			OutputBind outputBind) {
		StringBuilder ret = new StringBuilder();
		Variable variable = outputBind.variable;
		String inputObject = this.commonDefinitions.getVariableInName(variable);
		String destinationObject = outputBind.destinationObject.name;
		// If it is an object assignment, must declare the destination
		// object type and name.
		if (outputBind.outputBindType != OutputBindType.None) {
			ST st = new ST(templateCreateOutputAllocation);
			st.add("type", outputBind.destinationObject.typeName);
			st.add("name", outputBind.destinationObject.name);
			String baseType = outputBind.destinationObject.typeName
					.replaceAll("\\[", "").replaceAll("\\]", "").trim();
			st.add("baseType", baseType);
			st.add("inputAllocation", inputObject);
			ret.append(st.render());
		}
		ST st = new ST(templateAllocationCopyTo);
		st.add("inputObject", inputObject);
		st.add("destinationObject", destinationObject);
		ret.append(st.render());
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(String className, MethodCall methodCall) {
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaInterfaceImports() {
		ArrayList<String> ret = new ArrayList<>();
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaClassImports() {
		return this.getJavaInterfaceImports();
	}
}
