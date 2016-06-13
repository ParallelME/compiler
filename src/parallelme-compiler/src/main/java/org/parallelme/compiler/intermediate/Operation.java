/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler.intermediate;

import java.util.ArrayList;
import java.util.List;

import org.parallelme.compiler.symboltable.TokenAddress;

/**
 * Intermediate representation for operations.
 * 
 * @author Wilson de Carvalho
 */
public class Operation extends UserLibraryData {
	public enum ExecutionType {
		Parallel, Sequential, None;
	}

	public enum OperationType {
		Foreach, Reduce, Map, Filter;
	}

	public final TokenAddress statementAddress;
	public final OperationType operationType;
	public final Variable destinationVariable;

	private UserFunction userFunctionData;
	private ArrayList<Variable> externalVariables;

	private ExecutionType executionType;

	public Operation(Variable variableParameter, int sequentialNumber,
			TokenAddress statementAddress, OperationType operationType,
			Variable destinationVariable) {
		super(variableParameter, sequentialNumber);
		this.externalVariables = new ArrayList<>();
		this.setExecutionType(executionType);
		this.statementAddress = statementAddress;
		this.operationType = operationType;
		this.destinationVariable = destinationVariable;
	}

	public UserFunction getUserFunctionData() {
		return userFunctionData;
	}

	public void setUserFunctionData(UserFunction userFunctionData) {
		this.userFunctionData = userFunctionData;
	}

	public List<Variable> getExternalVariables() {
		return externalVariables;
	}

	public void addExternalVariable(Variable variable) {
		this.externalVariables.add(variable);
	}

	public ExecutionType getExecutionType() {
		return executionType;
	}

	public void setExecutionType(ExecutionType executionType) {
		this.executionType = executionType;
	}
}
