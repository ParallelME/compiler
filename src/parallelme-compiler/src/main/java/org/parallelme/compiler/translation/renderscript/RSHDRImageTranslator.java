/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.HDRImageTranslator;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Definitions for HDRImage translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public class RSHDRImageTranslator extends RSImageTranslator implements
		HDRImageTranslator {
	private static final String templateInputBindObjCreation = "Type <dataTypeInputObject> = new Type.Builder(<rsVarName>, Element.RGBA_8888(<rsVarName>))\n"
			+ "\t.setX(width)\n"
			+ "\t.setY(height)\n"
			+ "\t.create();\n"
			+ "Type <dataTypeOutputObject> = new Type.Builder(<rsVarName>, Element.F32_4(<rsVarName>))\n"
			+ "\t.setX(width)\n"
			+ "\t.setY(height)\n"
			+ "\t.create();\n"
			+ "<inputObject> = Allocation.createTyped(<rsVarName>, <dataTypeInputObject>, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);\n"
			+ "<outputObject> = Allocation.createTyped(<rsVarName>, <dataTypeOutputObject>);\n"
			+ "<inputObject>.copyFrom(data);\n"
			+ "<kernelName>.forEach_toFloat<classType>(<inputObject>, <outputObject>);";
	private static final String templateInputBind = "\nfloat4 __attribute__((kernel)) toFloat<classType>(uchar4 PM_in, uint32_t x, uint32_t y) {"
			+ "\n\tfloat4 PM_out;"
			+ "\n\tif (PM_in.s3 != 0) {"
			+ "\n\t\tfloat f = ldexp(1.0f, (PM_in.s3 & 0xFF) - (128 + 8));"
			+ "\n\t\tPM_out.s0 = (PM_in.s0 & 0xFF) * f;"
			+ "\n\t\tPM_out.s1 = (PM_in.s1 & 0xFF) * f;"
			+ "\n\t\tPM_out.s2 = (PM_in.s2 & 0xFF) * f;"
			+ "\n\t\tPM_out.s3 = 0.0f;"
			+ "\n\t} else {"
			+ "\n\t\tPM_out.s0 = 0.0f;"
			+ "\n\t\tPM_out.s1 = 0.0f;"
			+ "\n\t\tPM_out.s2 = 0.0f;"
			+ "\n\t\tPM_out.s3 = 0.0f;"
			+ "\n\t}"
			+ "\n\treturn PM_out;" + "\n}";
	private static final String templateOutputBind = "\nuchar4 __attribute__((kernel)) toBitmapHDRImage(float4 PM_in, uint32_t x, uint32_t y) {"
			+ "\n\tuchar4 PM_out;"
			+ "\n\tPM_out.r = (uchar) (PM_in.s0 * 255.0f);"
			+ "\n\tPM_out.g = (uchar) (PM_in.s1 * 255.0f);"
			+ "\n\tPM_out.b = (uchar) (PM_in.s2 * 255.0f);"
			+ "\n\tPM_out.a = 255;" + "\n\treturn PM_out;\n}";

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
	public String translateInputBindObjCreation(String className,
			InputBind inputBind) {
		String inputObject = commonDefinitions
				.getVariableInName(inputBind.variable);
		String outputObject = commonDefinitions
				.getVariableOutName(inputBind.variable);
		String dataTypeInputObject = inputObject + "DataType";
		String dataTypeOutputObject = outputObject + "DataType";
		ST st = new ST(templateInputBindObjCreation);
		st.add("params",
				commonDefinitions.toCommaSeparatedString(inputBind.parameters));
		st.add("dataTypeInputObject", dataTypeInputObject);
		st.add("dataTypeOutputObject", dataTypeOutputObject);
		st.add("inputObject", inputObject);
		st.add("outputObject", outputObject);
		st.add("kernelName", commonDefinitions.getKernelName(className));
		st.add("classType", inputBind.variable.typeName);
		st.add("rsVarName", getRSVariableName());
		return st.render();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBind(String className, OutputBind outputBind) {
		return templateOutputBind;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getJavaInterfaceImports() {
		ArrayList<String> ret = new ArrayList<>();
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
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(String className, MethodCall methodCall) {
		// TODO Throw an exception whenever a non supported method is provided.
		String ret = "return ";
		if (methodCall.methodName.equals(HDRImage.getInstance()
				.getHeightMethodName())) {
			ret += commonDefinitions.getVariableInName(methodCall.variable)
					+ ".getType().getY();";
		} else if (methodCall.methodName.equals(HDRImage.getInstance()
				.getWidthMethodName())) {
			ret += commonDefinitions.getVariableInName(methodCall.variable)
					+ ".getType().getX();";
		}
		return ret;
	}
}
