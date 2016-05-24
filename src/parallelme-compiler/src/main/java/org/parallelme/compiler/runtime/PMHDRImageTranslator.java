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
	private static final String templateInputBindCreation = "<imagePointer> = ParallelMERuntime.getInstance().createHDRImage(data, width, height);";
	private static final String templateToBitmap = "ParallelMERuntime.getInstance().toBitmapHDRImage(<imagePointer>, bitmap);";
	private static final String templateKernelToFloat = "__kernel void toFloat(__global uchar4 *PM_dataIn, __global float4 *PM_dataOut) {\n"
			+ "\tint PM_gid = get_global_id(0);\n"
			+ "\tuchar4 PM_in = PM_dataIn[PM_gid];\n"
			+ "\tfloat4 PM_out;\n"
			+ "\tfloat PM_f;\n"
			+ "\tif(PM_in.s3 != 0) {\n"
			+ "\t\tPM_f = ldexp(1.0f, (PM_in.s3 & 0xFF) - (128 + 8));\n"
			+ "\t\tPM_out.s0 = (PM_in.s0 & 0xFF) * PM_f;\n"
			+ "\t\tPM_out.s1 = (PM_in.s1 & 0xFF) * PM_f;\n"
			+ "\t\tPM_out.s2 = (PM_in.s2 & 0xFF) * PM_f;\n"
			+ "\t} else {\n"
			+ "\t\tPM_out.s0 = 0.0f;\n"
			+ "\t\tPM_out.s1 = 0.0f;\n"
			+ "\t\tPM_out.s2 = 0.0f;\n"
			+ "\t}\n"
			+ "\tPM_dataOut[PM_gid] = PM_out;\n"
			+ "}\n";
	private static final String templateKernelToBitmap = "__kernel void toBitmapHDRImage(__global float4 *PM_dataIn, __global uchar4 *PM_dataOut) {\n"
			+ "\tint PM_gid = get_global_id(0);\n"
			+ "\tfloat4 PM_in = PM_dataIn[PM_gid];\n"
			+ "\tuchar4 PM_out;\n"
			+ "\tPM_out.x = (uchar) (255.0f * PM_in.s0);\n"
			+ "\tPM_out.y = (uchar) (255.0f * PM_in.s1);\n"
			+ "\tPM_out.z = (uchar) (255.0f * PM_in.s2);\n"
			+ "\tPM_out.w = 255;\n"
			+ "\tPM_dataOut[PM_gid] = PM_out;\n" + "}\n";
	
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
