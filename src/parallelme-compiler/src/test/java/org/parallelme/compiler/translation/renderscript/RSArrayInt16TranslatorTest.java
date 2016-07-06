/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import org.parallelme.compiler.userlibrary.classes.Int16;

/**
 * Performs tests to validate RSArrayTranslator class for an user library Array
 * parametrized with an Int16.
 * 
 * @author Wilson de Carvalho
 */
public class RSArrayInt16TranslatorTest extends RSArrayTranslatorBaseTest {
	@Override
	protected String getParameterType() {
		return Int16.getInstance().getClassName();
	}

	@Override
	protected String getTranslatedParameterType() {
		return "short";
	}
	
	
	@Override
	protected String getRSType() {
		return "I16";
	}
}
