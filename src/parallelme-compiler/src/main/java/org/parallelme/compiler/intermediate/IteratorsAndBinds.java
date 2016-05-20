/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

import java.util.List;

/**
 * Container class for keeping collections of iterators and binds.
 * 
 * @author Wilson de Carvalho
 */
public class IteratorsAndBinds {
	public final List<InputBind> inputBinds;
	public final List<Iterator> iterators;
	public final List<OutputBind> outputBinds;

	public IteratorsAndBinds(List<InputBind> inputBinds,
			List<Iterator> iterators, List<OutputBind> outputBinds) {
		this.inputBinds = inputBinds;
		this.iterators = iterators;
		this.outputBinds = outputBinds;
	}
}
