/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
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
	private static final String templateMethodCall = "return ParallelMERuntime.getInstance().<methodName>(<params:{var|<var.name>}; separator=\", \">);";
	private static final String templateOutputBindCall1 = "bitmap = Bitmap.createBitmap(\n"
			+ "\tParallelMERuntime.getInstance().getWidth(<imagePointer>),\n"
			+ "\tParallelMERuntime.getInstance().getHeight(<imagePointer>),\n"
			+ "\tBitmap.Config.ARGB_8888);\n";
	private static final String templateOutputBindCall2 = "ParallelMERuntime.getInstance().toBitmap<className>(<imagePointer>, bitmap);";

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
		// If it is an object assignment, must declare the destination
		// object type and name.
		if (outputBind.outputBindType != OutputBindType.None) {
			ST st = new ST(templateOutputBindCall1);
			st.add("imagePointer",
					this.commonDefinitions.getPointerName(outputBind.variable));
			ret.append(st.render());
		}
		ST st = new ST(templateOutputBindCall2);
		st.add("imagePointer",
				this.commonDefinitions.getPointerName(outputBind.variable));
		st.add("className", outputBind.variable.typeName);
		ret.append(st.render());
		return ret.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(String className, MethodCall methodCall) {
		if (methodCall.methodName.equals(HDRImage.getInstance()
				.getHeightMethodName())
				|| methodCall.methodName.equals(HDRImage.getInstance()
						.getWidthMethodName())
				|| methodCall.methodName.equals(BitmapImage.getInstance()
						.getHeightMethodName())
				|| methodCall.methodName.equals(BitmapImage.getInstance()
						.getWidthMethodName())) {
			ST st = new ST(templateMethodCall);
			st.add("methodName", methodCall.methodName);
			st.addAggr("params.{name}",
					this.commonDefinitions.getPointerName(methodCall.variable));
			return st.render();
		}
		return "";
	}
}
