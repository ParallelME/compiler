/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.userlibrary.classes;

import java.util.HashMap;

import org.parallelme.compiler.userlibrary.UserLibraryDataType;

/**
 * Defines the user library class Int16.
 * 
 * @author Wilson de Carvalho
 */
public class Int16 extends UserLibraryDataType {
	private static Int16 instance = new Int16();
	private static final String className = "Int16";
	private static final String packageName = "org.parallelme.userlibrary.datatype";

	private Int16() {
		this.initValidMethodsSet();
	}

	public static Int16 getInstance() {
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTyped() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initValidMethodsSet() {
		this.validMethods = new HashMap<>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getClassName() {
		return className;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getPackageName() {
		return packageName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRenderScriptJavaType() {
		return "I16";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRenderScriptCType() {
		return "short";
	}
}
