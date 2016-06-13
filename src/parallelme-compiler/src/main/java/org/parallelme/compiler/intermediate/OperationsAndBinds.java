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
 * Container class for keeping collections of operations and binds.
 * 
 * @author Wilson de Carvalho
 */
public class OperationsAndBinds {
	public final List<InputBind> inputBinds;
	public final List<Operation> operations;
	public final List<OutputBind> outputBinds;

	public OperationsAndBinds(List<InputBind> inputBinds,
			List<Operation> operations, List<OutputBind> outputBinds) {
		this.inputBinds = inputBinds;
		this.operations = operations;
		this.outputBinds = outputBinds;
	}
}
