/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.renderscript;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.Variable;
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
	private static final String templateInputBindObjCreation = "Type <dataTypeInputObject> = new Type.Builder($mRS, Element.RGBA_8888($mRS))\n"
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
			+ "<kernelName>.forEach_toFloat<classType>(<inputObject>, <outputObject>);";
	private static final String templateInputBind = "\nfloat4 __attribute__((kernel)) toFloat<classType>(uchar4 $in, uint32_t x, uint32_t y) {"
			+ "\n\tfloat4 $out;"
			+ "\n\tif ($in.s3 != 0) {"
			+ "\n\t\tfloat f = ldexp(1.0f, ($in.s3 & 0xFF) - (128 + 8));"
			+ "\n\t\t$out.s0 = ($in.s0 & 0xFF) * f;"
			+ "\n\t\t$out.s1 = ($in.s1 & 0xFF) * f;"
			+ "\n\t\t$out.s2 = ($in.s2 & 0xFF) * f;"
			+ "\n\t} else {"
			+ "\n\t\t$out.s0 = 0.0f;"
			+ "\n\t\t$out.s1 = 0.0f;"
			+ "\n\t\t$out.s2 = 0.0f;"
			+ "\n\t}"
			+ "\n\treturn $out;"
			+ "\n}";

	public RSHDRImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBind(String className, InputBind inputBind) {
		ST st = new ST(templateInputBind);
		st.add("classType", inputBind.variable.typeName);
		return st.render();
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
		String outputObject = this.commonDefinitions
				.getVariableOutName(inputBind.variable);
		String dataTypeInputObject = this.commonDefinitions.getPrefix()
				+ inputBind.variable + "InDataType";
		String dataTypeOutputObject = this.commonDefinitions.getPrefix()
				+ inputBind.variable + "OutDataType";
		ST st = new ST(templateInputBindObjCreation);
		if (inputBind.parameters.length != 1
				|| !(inputBind.parameters[0] instanceof Variable)) {
			// TODO Throw exception here, once HDR must have 1 parameter
		}
		Variable variableParam = (Variable) inputBind.parameters[0];
		if (!variableParam.typeName.equals("RGBE.ResourceData")
				|| !variableParam.typeName.equals("ResourceData")) {
			// TODO Throw exception here, once parameter must be of type
			// ResourceData
		}
		st.add("resourceData", variableParam.name);
		st.add("params", this.commonDefinitions
				.toCommaSeparatedString(inputBind.parameters));
		st.add("dataTypeInputObject", dataTypeInputObject);
		st.add("dataTypeOutputObject", dataTypeOutputObject);
		st.add("inputObject", inputObject);
		st.add("outputObject", outputObject);
		st.add("kernelName", this.commonDefinitions.getKernelName(className));
		st.add("classType", inputBind.variable.typeName);
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaInterfaceImports() {
		ArrayList<String> ret = new ArrayList<>();
		ret.add("android.graphics.Bitmap");
		ret.add("org.parallelme.userlibrary.image.RGBE");
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
