/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Iterator;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.userlibrary.HDRImageTranslator;
import org.stringtemplate.v4.ST;

/**
 * Definitions for HDRImage translation to ParallelME runtime.
 * 
 * @author Wilson de Carvalho
 */
public class PMHDRImageTranslator extends PMImageTranslator implements
		HDRImageTranslator {
	private static final String templateCreateJavaAllocation = "RGBE.ResourceData <resourceData> = RGBE.loadFromResource(<params>);\n"
			+ "\t<varName>Worksize = <resourceData>.width * <resourceData>.height;\n"
			+ "\t<varName>ResourceDataId = <jniJavaClassName>.getInstance().getNewResourceId();";
	private static final String templateCreateAllocation = "int <varName>Worksize, <varName>ResourceDataId;\n";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBind(String className, InputBind inputBind) {
		// TODO Auto-generated method stub
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindCall(String className, InputBind inputBind) {
		// TODO Auto-generated method stub
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjCreation(String className,
			InputBind inputBind) {
		String resourceData = this.commonDefinitions.getPrefix()
				+ inputBind.getVariable() + "ResourceData";
		ST st = new ST(templateCreateJavaAllocation);
		st.add("resourceData", resourceData);
		st.add("params", this.commonDefinitions
				.toCommaSeparatedString(inputBind.getParameters()));
		st.add("varName",
				this.commonDefinitions.getPrefix()
						+ inputBind.getVariable().name);
		st.add("jniJavaClassName", this.getJNIWrapperClassName(className));
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjDeclaration(InputBind inputBind) {
		StringBuilder ret = new StringBuilder();
		ST st = new ST(templateCreateAllocation);
		st.add("varName",
				this.commonDefinitions.getPrefix()
						+ inputBind.getVariable().name);
		ret.append(st.render());
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBind(String className, OutputBind outputBind) {
		// TODO Auto-generated method stub
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateIterator(String className, Iterator iterator) {
		// TODO Auto-generated method stub
		return "";
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateIteratorCall(String className, Iterator iterator) {
		String jniJavaClassName = this.getJNIWrapperClassName(className);
		ST st = new ST(templateCallJNIFunction);
		st.add("jniJavaClassName", jniJavaClassName);
		st.add("functionName", this.commonDefinitions.getIteratorName(iterator));
		st.add("resourceDataId",
				this.getResourceDataIdName(iterator.getVariable().name));
		st.addAggr("params.{name}",
				this.getWorksizeName(iterator.getVariable().name));
		for (Variable variable : iterator.getExternalVariables()) {
			st.addAggr("params.{name}", variable.name);
		}
		return st.render() + ";";
	}
}
