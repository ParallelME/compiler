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
	private static final String templateInputBindCreation = "<imagePointer> = ParallelMERuntime().getInstance().createHDRImage(data, width, height);";
	private static final String templateToBitmap = "ParallelMERuntime().getInstance().toBitmapHDRImage(<imagePointer>, bitmap);";
	private static final String templateKernelToFloat = "__kernel void toFloat(__global uchar4 *$dataIn, __global float4 *$dataOut) {\n"
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
	private static final String templateKernelToBitmap = "__kernel void toBitmapHDRImage(__global float4 *$dataIn, __global uchar4 *$dataOut) {\n"
			+ "\tint $gid = get_global_id(0);\n"
			+ "\tfloat4 $in = $dataIn[$gid];\n"
			+ "\tuchar4 $out;\n"
			+ "\t$out.x = (uchar) (255.0f * $in.s0);\n"
			+ "\t$out.y = (uchar) (255.0f * $in.s1);\n"
			+ "\t$out.z = (uchar) (255.0f * $in.s2);\n"
			+ "\t$out.w = 255;\n"
			+ "\t$dataOut[$gid] = $out;\n" + "}\n";
	
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
	public String translateInputBindObjCreation(String className,
			InputBind inputBind) {
		ST st = new ST(templateInputBindCreation);
		st.add("imagePointer",
				this.commonDefinitions.getPointerName(inputBind.variable));
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
		return templateKernelToBitmap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String translateOutputBindCall(String className,
			OutputBind outputBind) {
		ST st = new ST(templateToBitmap);
		st.add("imagePointer",
				this.commonDefinitions.getPointerName(outputBind.variable));
		return st.render();
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
}
