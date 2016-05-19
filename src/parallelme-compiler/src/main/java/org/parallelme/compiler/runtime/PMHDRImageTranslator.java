/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.translation.CTranslator;
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
			+ "\t<worksize> = <resourceData>.width * <resourceData>.height;\n"
			+ "\t<inputBufferId> = ParallelMERuntimeJNIWrapper().getInstance().createAllocation(<resourceData>.data, 4 * <worksize>);\n"
			+ "\t<outputDataBuffer> = new float[<worksize>];\n"
			+ "\t<outputBufferId> = ParallelMERuntimeJNIWrapper().getInstance().createAllocation(<outputDataBuffer>, <worksize>);\n"
			+ "\tParallelMERuntimeJNIWrapper().getInstance().toFloat(<inputBufferId>, <outputBufferId>, <worksize>);\n";
	private static final String templateCreateAllocation = "int <worksize>, <inputBufferId>, <outputBufferId>;\n"
			+ "\tfloat[] <outputDataBuffer>;";

	public PMHDRImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBind(String className, InputBind inputBind) {
		return templateKernelToFloat;
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
				+ inputBind.variable + "Buffer";
		ST st = new ST(templateCreateJavaAllocation);
		st.add("resourceData", resourceData);
		st.add("params", this.commonDefinitions
				.toCommaSeparatedString(inputBind.parameters));
		st.add("worksize", this.getWorksizeName(inputBind.variable));
		st.add("inputBufferId", this.getInputBufferIdName(inputBind.variable));
		st.add("outputBufferId", this.getOutputBufferIdName(inputBind.variable));
		st.add("outputDataBuffer",
				this.getOutputBufferDataName(inputBind.variable));
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjDeclaration(InputBind inputBind) {
		StringBuilder ret = new StringBuilder();
		ST st = new ST(templateCreateAllocation);
		st.add("worksize", this.getWorksizeName(inputBind.variable));
		st.add("inputBufferId", this.getInputBufferIdName(inputBind.variable));
		st.add("outputBufferId", this.getOutputBufferIdName(inputBind.variable));
		st.add("outputDataBuffer",
				this.getOutputBufferDataName(inputBind.variable));
		ret.append(st.render());
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBind(String className, OutputBind outputBind) {
		return templateKernelToBitmap;
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
		ret.add("android.content.res.Resources");
		ret.add("android.graphics.Bitmap");
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaClassImports() {
		ArrayList<String> ret = (ArrayList<String>) this
				.getJavaInterfaceImports();
		ret.add("org.parallelme.userlibrary.image.RGBE");
		return ret;
	}
}
