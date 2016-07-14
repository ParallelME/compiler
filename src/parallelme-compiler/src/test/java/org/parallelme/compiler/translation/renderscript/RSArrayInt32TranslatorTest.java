/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.renderscript;

import org.parallelme.compiler.userlibrary.classes.Float32;
import org.parallelme.compiler.userlibrary.classes.Int32;

/**
 * Performs tests to validate RSArrayTranslator class for an user library Array
 * parametrized with an Int32.
 * 
 * @author Wilson de Carvalho
 */
public class RSArrayInt32TranslatorTest extends RSArrayTranslatorBaseTest {
	@Override
	protected String getParameterType() {
		return Int32.getInstance().getClassName();
	}

	@Override
	protected String getMapType() {
		return Float32.getInstance().getClassName();
	}

	@Override
	protected String getTranslatedParameterType() {
		return "int";
	}
	
	@Override
	protected String getTranslatedMapType() {
		return "float";
	}

	@Override
	protected String getRSType() {
		return "I32";
	}
	
	@Override
	protected String getMapRSType() {
		return "F32";
	}
}
