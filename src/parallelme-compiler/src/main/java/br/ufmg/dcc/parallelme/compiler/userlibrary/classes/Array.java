/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.userlibrary.classes;

import java.util.HashSet;

import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryCollectionClassImpl;

/**
 * Defines the user library collection class Array.
 * 
 * @author Wilson de Carvalho
 */
public class Array extends UserLibraryCollectionClassImpl {
	private static String iteratorMethodName = "foreach";
	private static String dataOutputMethodName = "toJavaArray";
	private static Array instance = new Array();

	private Array() {
		this.initValidMethodsSet();
	}
	
	public static Array getInstance() {
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initValidMethodsSet() {
		this.validMethods = new HashSet<>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTyped() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIteratorMethodName() {
		return iteratorMethodName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDataOutputMethodName() {
		return dataOutputMethodName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public static String getName() {
		return "Array";
	}
}
