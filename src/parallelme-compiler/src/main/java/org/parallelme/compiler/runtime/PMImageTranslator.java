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
			ST st = new ST(templateCallJNIFunction);
			st.add("jniJavaClassName", jniJavaClassName);
			st.add("functionName", "toBitmap");
			st.add("resourceDataId", this.getResourceDataName(variable.name));
			String workSize = this.getWorksizeName(variable.name);
			st.addAggr("params.{name}", workSize);
			st.addAggr("params.{name}", this.getResourceDataName(variable.name));
			// 4 slots. [Red][Green][Blue][Alpha]
			st.addAggr("params.{name}", workSize + " * 4");
			st.addAggr("params.{name}", outputBind.destinationObject.name);
			// sizeof(float) = 4 * (4 slots)
			st.addAggr("params.{name}", workSize + " * 16");
			ret.append(st.render());
		}
		return ret.toString();
	}
}
