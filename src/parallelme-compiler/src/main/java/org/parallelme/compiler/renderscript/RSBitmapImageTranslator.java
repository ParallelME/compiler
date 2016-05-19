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
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.BitmapImageTranslator;
import org.stringtemplate.v4.ST;

/**
 * Definitions for BitmapImage translation to RenderScript runtime.
 * 
 * @author Wilson de Carvalho
 */
public class RSBitmapImageTranslator extends RSImageTranslator implements
		BitmapImageTranslator {
	private static final String templateCreateAllocation = "Type <dataTypeInputObject>;\n"
			+ "<inputObject> = Allocation.createFromBitmap($mRS, <param>, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT | Allocation.USAGE_SHARED);\n"
			+ "<dataTypeInputObject> = new Type.Builder($mRS, Element.F32_3($mRS))\n"
			+ "\t.setX(<inputObject>.getType().getX())\n"
			+ "\t.setY(<inputObject>.getType().getY())\n"
			+ "\t.create();\n"
			+ "<outputObject> = Allocation.createTyped($mRS, <dataTypeInputObject>);\n"
			+ "<kernelName>.forEach_toFloat(<inputObject>, <outputObject>);";

	public RSBitmapImageTranslator(CTranslator cCodeTranslator) {
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
		String inputObject = this.commonDefinitions
				.getVariableInName(inputBind.variable);
		String outputObject = this.commonDefinitions
				.getVariableOutName(inputBind.variable);
		String dataTypeInputObject = this.commonDefinitions.getPrefix()
				+ inputBind.variable + "InDataType";
		ST st = new ST(templateCreateAllocation);
		st.add("dataTypeInputObject", dataTypeInputObject);
		st.add("inputObject", inputObject);
		st.add("outputObject", outputObject);
		st.add("param", inputBind.parameters[0]);
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
		return this.getJavaInterfaceImports();
	}
}
