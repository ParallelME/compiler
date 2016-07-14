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
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.BitmapImageTranslator;
import org.stringtemplate.v4.ST;

/**
 * Definitions for HDRImage translation to ParallelME runtime.
 * 
 * @author Wilson de Carvalho
 */
public class PMBitmapImageTranslator extends PMImageTranslator implements
		BitmapImageTranslator {
	private static final String templateInputBindObjCreation = "<imagePointer> = ParallelMERuntime.getInstance().createBitmapImage(<bitmapName>);";
	private static final String templateKernelToFloat = "__kernel void toFloatBitmapImage(__global uchar4 *PM_dataIn, __global float4 *PM_dataOut) {\n"
			+ "\tint PM_gid = get_global_id(0);\n"
			+ "\tuchar4 PM_in = PM_dataIn[PM_gid];\n"
			+ "\tfloat3 PM_out;\n"
			+ "\tPM_out.s0 = (float) PM_in.s0;\n"
			+ "\tPM_out.s1 = (float) PM_in.s1;\n"
			+ "\tPM_out.s2 = (float) PM_in.s2;\n"
			+ "\tPM_out.s3 = 0f;\n"
			+ "\tPM_dataOut[PM_gid] = PM_out;\n"
			+ "}\n";
	private static final String templateKernelToBitmap = "__kernel void toBitmapBitmapImage(__global float4 *PM_dataIn, __global uchar4 *PM_dataOut) {\n"
			+ "\tint PM_gid = get_global_id(0);\n"
			+ "\tfloat4 PM_in = PM_dataIn[PM_gid];\n"
			+ "\tuchar4 PM_out;\n"
			+ "\tPM_out.x = (uchar) PM_in.s0;\n"
			+ "\tPM_out.y = (uchar) PM_in.s1;\n"
			+ "\tPM_out.z = (uchar) PM_in.s2;\n"
			+ "\tPM_out.w = 255;\n"
			+ "\tPM_dataOut[PM_gid] = PM_out;\n" + "}\n";
	
	public PMBitmapImageTranslator(CTranslator cCodeTranslator) {
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
		ST st = new ST(templateInputBindObjCreation);
		st.add("imagePointer",
				this.commonDefinitions.getPointerName(inputBind.variable));
		Variable variable = (Variable) inputBind.parameters.get(0);
		st.add("bitmapName", variable.name);
		return st.render();
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
