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

import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassImpl;

/**
 * Defines the user library class Int32.
 * 
 * @author Wilson de Carvalho
 */
public class Int32 extends UserLibraryClassImpl {
	private static Int32 instance = new Int32();

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
		this.validMethods = new HashSet<>();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public static String getName() {
		return "Int32";
	}
}
