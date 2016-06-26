/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.ArrayTranslator;
import org.stringtemplate.v4.ST;

/**
 * Definitions for Array translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public class PMArrayTranslator extends PMTranslator implements ArrayTranslator {
	private static final String templateInputBindObjCreation = "<arrayPointer> = ParallelMERuntime.getInstance().createArray(<arrayName>);";
	private static final String templateOutputBindCall1 = "<name> = (<type>) java.lang.reflect.Array.newInstance(<baseType>.class,\n"
			+ "\tParallelMERuntime.getInstance().getLength(<arrayPointer>));\n";
	private static final String templateOutputBindCall2 = "ParallelMERuntime.getInstance().toArray(<arrayPointer>, <arrayName>);";

	public PMArrayTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
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
		ST st = new ST(templateInputBindObjCreation);
		st.add("arrayPointer",
				this.commonDefinitions.getPointerName(inputBind.variable));
		Variable variable = (Variable) inputBind.parameters.get(0);
		st.add("arrayName", variable.name);
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjDeclaration(InputBind inputBind) {
		return "";
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
		String arrayPointer = this.commonDefinitions
				.getPointerName(outputBind.variable);
		// If it is an object assignment, must declare the destination
		// object type and name.
		if (outputBind.outputBindType != OutputBindType.None) {
			ST st = new ST(templateOutputBindCall1);
			st.add("type", outputBind.destinationObject.typeName);
			st.add("name", outputBind.destinationObject.name);
			String baseType = outputBind.destinationObject.typeName
					.replaceAll("\\[", "").replaceAll("\\]", "").trim();
			st.add("arrayPointer", arrayPointer);
			st.add("baseType", baseType);
			ret.append(st.render());
		}
		ST st = new ST(templateOutputBindCall2);
		st.add("arrayPointer", arrayPointer);
		st.add("arrayName", outputBind.destinationObject.name);
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
