/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler.runtime.translationdata;

import java.util.ArrayList;

import br.ufmg.dcc.parallelme.compiler.symboltable.TokenAddress;

/**
 * Stores iterator data that must be translated to the targed runtime.
 * 
 * @author Wilson de Carvalho
 */
public class Iterator extends UserLibraryData {
	private UserFunction userFunctionData;
	private ArrayList<Variable> functionVariables;
	private TokenAddress statementAddress;

	public Iterator(Variable variableParameter,
			int sequentialNumber, TokenAddress statementAddress) {
		super(variableParameter, sequentialNumber);
		this.functionVariables = new ArrayList<>();
		this.setStatementAddress(statementAddress);
	}

	public UserFunction getUserFunctionData() {
		return userFunctionData;
	}

	public void setUserFunctionData(UserFunction userFunctionData) {
		this.userFunctionData = userFunctionData;
	}

	public ArrayList<Variable> getFunctionVariables() {
		return functionVariables;
	}

	public void addFunctionVariable(Variable variable) {
		this.functionVariables.add(variable);
	}

	public TokenAddress getStatementAddress() {
		return statementAddress;
	}

	public void setStatementAddress(TokenAddress statementAddress) {
		this.statementAddress = statementAddress;
	}
}
