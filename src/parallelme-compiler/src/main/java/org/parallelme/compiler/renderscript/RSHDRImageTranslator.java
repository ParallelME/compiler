/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.renderscript;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.HDRImageTranslator;
import org.stringtemplate.v4.ST;

/**
 * Definitions for HDRImage translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public class RSHDRImageTranslator extends RSImageTranslator implements
		HDRImageTranslator {
	private static final String templateCreateAllocation = "RGBE.ResourceData <resourceData> = RGBE.loadFromResource(<params>);\n"
			+ "Type <dataTypeInputObject> = new Type.Builder($mRS, Element.RGBA_8888($mRS))\n"
			+ "\t.setX(<resourceData>.width)\n"
			+ "\t.setY(<resourceData>.height)\n"
			+ "\t.create();\n"
			+ "Type <dataTypeOutputObject> = new Type.Builder($mRS, Element.F32_4($mRS))\n"
			+ "\t.setX(<resourceData>.width)\n"
			+ "\t.setY(<resourceData>.height)\n"
			+ "\t.create();\n"
			+ "<inputObject> = Allocation.createTyped($mRS, <dataTypeInputObject>, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);\n"
			+ "<outputObject> = Allocation.createTyped($mRS, <dataTypeOutputObject>);\n"
			+ "<inputObject>.copyFrom(<resourceData>.data);\n"
			+ "<kernelName>.forEach_toFloat(<inputObject>, <outputObject>);";

	public RSHDRImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
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
		String inputObject = this.commonDefinitions.getVariableInName(inputBind
				.getVariable());
		String outputObject = this.commonDefinitions
				.getVariableOutName(inputBind.getVariable());
		String dataTypeInputObject = this.commonDefinitions.getPrefix()
				+ inputBind.getVariable() + "InDataType";
		String dataTypeOutputObject = this.commonDefinitions.getPrefix()
				+ inputBind.getVariable() + "OutDataType";
		String resourceData = this.commonDefinitions.getPrefix()
				+ inputBind.getVariable() + "ResourceData";
		ST st = new ST(templateCreateAllocation);
		st.add("resourceData", resourceData);
		st.add("params", this.commonDefinitions
				.toCommaSeparatedString(inputBind.getParameters()));
		st.add("dataTypeInputObject", dataTypeInputObject);
		st.add("dataTypeOutputObject", dataTypeOutputObject);
		st.add("inputObject", inputObject);
		st.add("outputObject", outputObject);
		st.add("kernelName", this.commonDefinitions.getKernelName(className));
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBind(String className, OutputBind outputBind) {
		return "";
	}
}