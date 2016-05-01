/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.runtime;

import org.parallelme.compiler.translation.userlibrary.BaseTranslator;

/**
 * Base class for ParallelME runtime translators.
 * 
 * @author Wilson de Carvalho
 */
public abstract class PMTranslator extends BaseTranslator {
	protected static final String templateCallJNIFunction = "<jniJavaClassName>.getInstance().<functionName>(<resourceDataId><params:{var|, <var.name>}>)";

	protected String getJNIWrapperClassName(String className) {
		return className + "JNIWrapper";
	}

	protected String getResourceDataName(String variableName) {
		return this.commonDefinitions.getPrefix() + variableName
				+ "ResourceData";
	}

	protected String getResourceDataIdName(String variableName) {
		return this.commonDefinitions.getPrefix() + variableName
				+ "ResourceDataId";
	}

	protected String getWorksizeName(String variableName) {
		return this.commonDefinitions.getPrefix() + variableName + "Worksize";
	}
}
