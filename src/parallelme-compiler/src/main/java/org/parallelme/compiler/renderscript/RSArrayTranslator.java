/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.renderscript;

import java.util.HashMap;
import java.util.Map;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.OutputBind;
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
	private static final String templateCreateAllocation = "<allocation> = Allocation.createSized($mRS, Element.<elementType>($mRS), <allocationLength>);\n"
			+ "<allocation>.copyFrom(<inputArray>);";
	private static final String templateCreateOutputAllocation = "<type> <name> = (<type>) java.lang.reflect.Array.newInstance(<baseType>.class, <inputAllocation>.getType().getX());\n";
	private static final String templateAllocationCopyTo = "<inputObject>.copyTo(<destinationObject>);\n";

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
	public String translateInputBindCall(String className, InputBind inputBind) {
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
		// TODO Check if parameters array has size 3, otherwise throw an
		// exception and abort translation.
		st.add("inputArray", inputBind.parameters[0]);
		st.add("allocationLength", inputBind.parameters[2]);
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
		return "Allocation " + inAllocation + ";\n";
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
		// If it is an object declaration, must declare the destination
		// object type and name.
		if (outputBind.isObjectDeclaration) {
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
}
