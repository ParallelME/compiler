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
 * Defines the user library class Pixel.
 * 
 * @author Wilson de Carvalho
 */
public class Pixel extends UserLibraryDataType {
	private static Pixel instance = new Pixel();
	private static final String className = "Pixel";
	private static final String packageName = "org.parallelme.userlibrary.image";

	private Pixel() {
		this.initValidMethodsSet();
	}

	public static Pixel getInstance() {
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
		return "F32_4";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRenderScriptCType() {
		return "float4";
	}
}
