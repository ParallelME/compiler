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
 * Defines the user library class Int32.
 * 
 * @author Wilson de Carvalho
 */
public class Int32 extends UserLibraryDataType {
	private static Int32 instance = new Int32();
	private static final String className = "Int32";
	private static final String packageName = "org.parallelme.userlibrary.datatype";

	private Int32() {
		this.initValidMethodsSet();
	}

	public static Int32 getInstance() {
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
		return "I32";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCType() {
		return "int";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getJavaPrimitiveType() {
		return "int";
	}
}
