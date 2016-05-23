/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.HDRImageTranslator;
import org.parallelme.compiler.userlibrary.classes.HDRImage;
import org.stringtemplate.v4.ST;

/**
 * Definitions for Image translation to ParallelME runtime.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMImageTranslator extends PMTranslator implements
		HDRImageTranslator {
	private static final String templateMethodCall = "return ParallelMERuntime().getInstance().<methodName>(<params:{var|<var.name>}; separator=\", \">);";

	public PMImageTranslator(CTranslator cCodeTranslator) {
		super(cCodeTranslator);
	}

	/**
	 * {@inheritDoc}
	 */
	public String translateMethodCall(String className, MethodCall methodCall) {
		if (methodCall.methodName.equals(HDRImage.getInstance()
				.getHeightMethodName())
				|| methodCall.methodName.equals(HDRImage.getInstance()
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
