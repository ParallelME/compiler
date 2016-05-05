/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.HDRImageTranslator;
import org.parallelme.compiler.userlibrary.classes.BitmapImage;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Definitions for Image translation to ParallelME runtime.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMImageTranslator extends PMTranslator implements
		HDRImageTranslator {
	protected static final String templateKernelToFloat = "__kernel void toFloat(__global uchar4 *$dataIn, __global float4 *$dataOut) {\n"
			+ "\tint $gid = get_global_id(0);\n"
			+ "\tuchar4 $in = $dataIn[$gid];\n"
			+ "\tfloat4 $out;\n"
			+ "\tfloat $f;\n"
			+ "\tif($in.s3 != 0) {\n"
			+ "\t\t$f = ldexp(1.0f, ($in.s3 & 0xFF) - (128 + 8));\n"
			+ "\t\t$out.s0 = ($in.s0 & 0xFF) * $f;\n"
			+ "\t\t$out.s1 = ($in.s1 & 0xFF) * $f;\n"
			+ "\t\t$out.s2 = ($in.s2 & 0xFF) * $f;\n"
			+ "\t} else {\n"
			+ "\t\t$out.s0 = 0.0f;\n"
			+ "\t\t$out.s1 = 0.0f;\n"
			+ "\t\t$out.s2 = 0.0f;\n"
			+ "\t}\n"
			+ "\t$dataOut[$gid] = $out;\n"
			+ "}\n";
	protected static final String templateKernelToBitmap = "__kernel void toBitmap(__global float4 *$dataIn, __global uchar4 *$dataOut) {\n"
			+ "\tint $gid = get_global_id(0);\n"
			+ "\tfloat4 $in = $dataIn[$gid];\n"
			+ "\tuchar4 $out;\n"
			+ "\t$out.x = (uchar) ($in.s0);\n"
			+ "\t$out.y = (uchar) ($in.s1);\n"
			+ "\t$out.z = (uchar) ($in.s2);\n"
			+ "\t$out.w = <alphaValue>;\n"
			+ "\t$dataOut[$gid] = $out;\n" + "}\n";
	protected static final String templateBitmapBuffer = "int <bitmapBufferId> = ParallelMERuntimeJNIWrapper().getInstance().createAllocation(<bitmapVariable>, <worksize>);\n";

	public PMImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBindCall(String className,
			OutputBind outputBind) {
		StringBuilder ret = new StringBuilder();
		Variable variable = outputBind.getVariable();
		String jniJavaClassName = this.getJNIWrapperClassName(className);
		if (variable.typeName.equals(BitmapImage.getName())
				|| variable.typeName.equals(HDRImage.getName())) {
			ST st = new ST(templateBitmapBuffer);
			st.add("bitmapBufferId", this.getBitmapBufferName(variable));
			st.add("bitmapVariable", outputBind.destinationObject.name);
			st.add("worksize", this.getWorksizeName(variable));
			ret.append(st.render());
			st = new ST(templateCallJNIFunction);
			st.add("jniJavaClassName", jniJavaClassName);
			st.add("functionName", "toBitmap");
			st.addAggr("params.{name}", this.getOutputBufferDataName(variable));
			st.addAggr("params.{name}", this.getBitmapBufferName(variable));
			st.addAggr("params.{name}", this.getWorksizeName(variable));
			ret.append("\t" + st.render());
			st = new ST(templateRunWait);
			st.add("bufferId", this.getBitmapBufferName(variable));
			ret.append(st.render());
		}
		return ret.toString();
	}

	protected String getBitmapBufferName(Variable variable) {
		return this.commonDefinitions.getPrefix() + variable.name
				+ "BitmapBuffer";
	}
}
