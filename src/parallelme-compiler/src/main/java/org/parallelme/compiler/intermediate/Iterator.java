/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

import java.util.ArrayList;

import org.parallelme.compiler.symboltable.TokenAddress;

/**
 * Intermediate representation for iterators.
 * 
 * @author Wilson de Carvalho
 */
public class Iterator extends UserLibraryData {
	public enum IteratorType {
		Parallel, Sequential, None;
	}

	private UserFunction userFunctionData;
	private ArrayList<Variable> externalVariables;
	public final TokenAddress statementAddress;
	private IteratorType type;

	public Iterator(Variable variableParameter, int sequentialNumber,
			TokenAddress statementAddress) {
		super(variableParameter, sequentialNumber);
		this.externalVariables = new ArrayList<>();
		this.setType(type);
		this.statementAddress = statementAddress;
	}

	public UserFunction getUserFunctionData() {
		return userFunctionData;
	}

	public void setUserFunctionData(UserFunction userFunctionData) {
		this.userFunctionData = userFunctionData;
	}

	public Variable[] getExternalVariables() {
		Variable[] ret = new Variable[externalVariables.size()];
		ret = externalVariables.toArray(ret);
		return ret;
	}

	public void addExternalVariable(Variable variable) {
		this.externalVariables.add(variable);
	}

	public IteratorType getType() {
		return type;
	}

	public void setType(IteratorType type) {
		this.type = type;
	}
}
