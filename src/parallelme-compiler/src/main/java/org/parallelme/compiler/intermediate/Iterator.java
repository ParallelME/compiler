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
public class Iterator extends UserLibraryData implements Cloneable {
	public enum IteratorType {
		Parallel, Sequential, None;
	}

	private UserFunction userFunctionData;
	private ArrayList<Variable> externalVariables;
	private TokenAddress statementAddress;
	private IteratorType type;
	private Iterator enclosingIterator;

	public Iterator(Variable variableParameter, int sequentialNumber,
			TokenAddress statementAddress, Iterator enclosingIterator) {
		super(variableParameter, sequentialNumber);
		this.externalVariables = new ArrayList<>();
		this.setType(type);
		this.setStatementAddress(statementAddress);
		this.enclosingIterator = enclosingIterator;
	}
	
	public Iterator getEnclosingIterator(){
		return this.enclosingIterator;
	}

	public UserFunction getUserFunctionData() {
		return userFunctionData;
	}

	public void setUserFunctionData(UserFunction userFunctionData) {
		this.userFunctionData = userFunctionData;
	}

	public ArrayList<Variable> getExternalVariables() {
		return externalVariables;
	}

	public void addExternalVariable(Variable variable) {
		this.externalVariables.add(variable);
	}

	public TokenAddress getStatementAddress() {
		return statementAddress;
	}

	public void setStatementAddress(TokenAddress statementAddress) {
		this.statementAddress = statementAddress;
	}

	public IteratorType getType() {
		return type;
	}

	public void setType(IteratorType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		return this.userFunctionData.hashCode() * 3
				+ this.externalVariables.hashCode() * 7
				+ this.statementAddress.hashCode() * 11 + type.hashCode() * 13;
	}
	
	public Iterator clone(){  
	    try{  
	        return (Iterator) super.clone();  
	    }catch(Exception e){ 
	        return null; 
	    }
	}
}
