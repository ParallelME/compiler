/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.translation.runtime;

import org.parallelme.compiler.userlibrary.classes.Int16;
import org.parallelme.compiler.userlibrary.classes.Int32;

/**
 * Performs tests to validate PMArrayTranslator class for an user library Array
 * parametrized with an Int16.
 * 
 * @author Wilson de Carvalho
 */
public class PMArrayInt16TranslatorTest extends PMArrayTranslatorBaseTest {
	@Override
	protected String getParameterType() {
		return Int16.getInstance().getClassName();
	}
	
	@Override
	protected String getMapType() {
		return Int32.getInstance().getClassName();
	}

	@Override
	protected String getTranslatedParameterType() {
		return "short";
	}

	@Override
	protected String getTranslatedMapType() {
		return "int";
	}
}
