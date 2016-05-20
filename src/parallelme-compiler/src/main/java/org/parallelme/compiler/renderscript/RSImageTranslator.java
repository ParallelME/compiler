/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.renderscript;

import org.parallelme.compiler.intermediate.InputBind;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.ImageTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Base class for Image translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public abstract class RSImageTranslator extends RSTranslator implements
		ImageTranslator {
	private static final String templateOutputBindCall1 = "<destinationObject> = Bitmap.createBitmap(<inputAllocation>.getType().getX(), <inputAllocation>.getType().getY(), Bitmap.Config.ARGB_8888);\n";
	private static final String templateOutputBindCall2 = "<kernelName>.forEach_toBitmap<classType>(<outputObject>, <inputObject>);\n"
			+ "<inputObject>.copyTo(<destinationObject>);";
	private static final String templateOutputBind = "\nuchar4 __attribute__((kernel)) toBitmap<classType>(<varType>"
			+ " $in, uint32_t x, uint32_t y) {"
			+ "\n\tuchar4 $out;"
			+ "\n\t$out.r = (uchar) ($in.s0<multiplyBy>);"
			+ "\n\t$out.g = (uchar) ($in.s1<multiplyBy>);"
			+ "\n\t$out.b = (uchar) ($in.s2<multiplyBy>);"
			+ "\n\t$out.a = 255;" + "\n\treturn $out;\n}";

	public RSImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateInputBindObjDeclaration(InputBind inputBind) {
		String inAllocation = this.commonDefinitions
				.getVariableInName(inputBind.variable);
		String outAllocation = this.commonDefinitions
				.getVariableOutName(inputBind.variable);
		return "Allocation " + inAllocation + ", " + outAllocation + ";";
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
		String outputObject = this.commonDefinitions
				.getVariableOutName(variable);
		String destinationObject = outputBind.destinationObject.name;
		// If it is an object assignment, must declare the destination
		// object type and name.
		if (outputBind.outputBindType != OutputBindType.None) {
			ST st = new ST(templateOutputBindCall1);
			st.add("inputAllocation", inputObject);
			st.add("destinationObject", destinationObject);
			ret.append(st.render());
		}
		ST st = new ST(templateOutputBindCall2);
		st.add("classType", outputBind.variable.typeName);
		st.add("kernelName", this.commonDefinitions.getKernelName(className));
		st.add("outputObject", outputObject);
		st.add("inputObject", inputObject);
		st.add("destinationObject", destinationObject);
		ret.append(st.render());

		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBind(String className, OutputBind outputBind) {
		String ret = "";
		String typeName = outputBind.variable.typeName;
		if (typeName.equals(BitmapImage.getName())
				|| typeName.equals(HDRImage.getName())) {
			String varType;
			ST st = new ST(templateOutputBind);
			st.add("classType", outputBind.variable.typeName);
			if (typeName.equals(HDRImage.getName())) {
				st.add("multiplyBy", " * 255.0f");
				varType = "float4";
			} else {
				st.add("multiplyBy", null);
				varType = "float3";
			}
			st.add("varType", varType);
			ret = st.render();
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(String className, MethodCall methodCall) {
		// TODO Throw an exception whenever an non supported method is provided.
		String ret = "return ";
		if (methodCall.variable.typeName.equals(BitmapImage.getName())) {
			if (methodCall.methodName.equals(BitmapImage.getInstance()
					.getHeightMethodName())) {
				ret += this.commonDefinitions
						.getVariableInName(methodCall.variable)
						+ ".getType().getY();";
			} else if (methodCall.methodName.equals(BitmapImage.getInstance()
					.getWidthMethodName())) {
				ret += this.commonDefinitions
						.getVariableInName(methodCall.variable)
						+ ".getType().getX();";
			}
		} else if (methodCall.variable.typeName.equals(HDRImage.getName())) {
			if (methodCall.methodName.equals(HDRImage.getInstance()
					.getHeightMethodName())) {
				ret += this.commonDefinitions
						.getVariableInName(methodCall.variable)
						+ ".getType().getY();";
			} else if (methodCall.methodName.equals(HDRImage.getInstance()
					.getWidthMethodName())) {
				ret += this.commonDefinitions
						.getVariableInName(methodCall.variable)
						+ ".getType().getX();";
			}
		}
		return ret;
	}
}
