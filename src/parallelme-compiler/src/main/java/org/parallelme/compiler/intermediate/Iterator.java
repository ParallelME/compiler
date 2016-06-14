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
import org.antlr.v4.runtime.TokenStream;

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
	private ArrayList<Iterator> nestedIterators;
	
	private TokenStream tokenStream; //Just Token Address is not enough

	public TokenStream getTokenStream() {
		return tokenStream;
	}

	public void setTokenStream(TokenStream tokenStream) {
		this.tokenStream = tokenStream;
	}

	public Iterator(Variable variableParameter, int sequentialNumber,
			TokenAddress statementAddress, Iterator enclosingIterator, TokenStream tokenStream) {
		super(variableParameter, sequentialNumber);
		this.externalVariables = new ArrayList<>();
		this.setType(type);
		this.setStatementAddress(statementAddress);
		this.tokenStream = tokenStream;
		this.nestedIterators = new ArrayList<Iterator>();
		if(enclosingIterator != null){
			this.enclosingIterator = enclosingIterator;
			enclosingIterator.addNestedIterator(this);
		}
	}
	
	public Iterator getEnclosingIterator(){
		return this.enclosingIterator;
	}
	
	public void addNestedIterator(Iterator nestedIterator){
		this.nestedIterators.add(nestedIterator);
	}
	
	public ArrayList<Iterator> getNestedIterators(){
		return this.nestedIterators;
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
