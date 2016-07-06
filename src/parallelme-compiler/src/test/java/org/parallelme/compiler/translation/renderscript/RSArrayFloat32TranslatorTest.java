/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import org.parallelme.compiler.userlibrary.classes.Float32;

/**
 * Performs tests to validate RSArrayTranslator class for an user library Array
 * parametrized with an Float32.
 * 
 * @author Wilson de Carvalho
 */
public class RSArrayFloat32TranslatorTest extends RSArrayTranslatorBaseTest {
	@Override
	protected String getParameterType() {
		return Float32.getInstance().getClassName();
	}

	@Override
	protected String getTranslatedParameterType() {
		return "float";
	}
	
	
	@Override
	protected String getRSType() {
		return "F32";
	}
}
