/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
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
	private static final String templateInputBindObjCreation = "<allocation> = Allocation.createSized(PM_mRS, Element.<elementType>(PM_mRS), <inputArray>.length);\n"
			+ "<allocation>.copyFrom(<inputArray>);";
	private static final String templateOutputBindCall1 = "<name> = (<type>) java.lang.reflect.Array.newInstance(<baseType>.class, <inputAllocation>.getType().getX());\n";
	private static final String templateOutputBindCall2 = "<inputObject>.copyTo(<destinationObject>);";

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
		parallelME2RSAllocationTypes.put(Int16.getInstance().getClassName(),
				"I16");
		parallelME2RSAllocationTypes.put(Int32.getInstance().getClassName(),
				"I32");
		parallelME2RSAllocationTypes.put(Float32.getInstance().getClassName(),
				"F32");
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
		ST st = new ST(templateInputBindObjCreation);
		// TODO Check if parameters array has size 1, otherwise throw an
		// exception and abort translation.
		st.add("inputArray", inputBind.parameters.get(0));
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
		String inputObject = this.commonDefinitions
				.getVariableInName(outputBind.variable);
		String destinationObject = outputBind.destinationObject.name;
		// If it is an object assignment, must declare the destination
		// object type and name.
		if (outputBind.outputBindType != OutputBindType.None) {
			ST st = new ST(templateOutputBindCall1);
			st.add("type", outputBind.destinationObject.typeName);
			st.add("name", outputBind.destinationObject.name);
			String baseType = outputBind.destinationObject.typeName
					.replaceAll("\\[", "").replaceAll("\\]", "").trim();
			st.add("baseType", baseType);
			st.add("inputAllocation", inputObject);
			ret.append(st.render());
		}
		ST st = new ST(templateOutputBindCall2);
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

	@Override
	protected String getDestinationArraySize(Operation operation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getReturnObjectCreation(Operation operation,
			String variableName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void fillReduceOperationCall(ST st, Operation operation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void fillForeachOperationCall(ST st, Operation operation) {
		// TODO Auto-generated method stub
		
	}
}
