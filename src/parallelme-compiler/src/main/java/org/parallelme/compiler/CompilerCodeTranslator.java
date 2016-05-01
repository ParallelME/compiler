/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.symboltable.*;
import org.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import org.parallelme.compiler.userlibrary.UserLibraryCollectionClass;
import org.parallelme.compiler.util.FileWriter;
import org.parallelme.compiler.util.Pair;

/**
 * Translates the user code written with the user library to a runtime
 * compatible code.
 * 
 * @author Wilson de Carvalho
 */
public class CompilerCodeTranslator {
	private int functionsCount = 0;
	private final RuntimeDefinition runtime;
	private final String outputDestinationFolder;
	private final RuntimeCommonDefinitions commondDefinitions;

	/**
	 * Base constructor.
	 * 
	 * @param runtime
	 *            Selected runtime for output code.
	 * @param outputDestinationFolder
	 *            Output destination folder for compiled files.
	 */
	public CompilerCodeTranslator(RuntimeDefinition runtime,
			String outputDestinationFolder) {
		this.runtime = runtime;
		this.outputDestinationFolder = outputDestinationFolder;
		this.commondDefinitions = new RuntimeCommonDefinitions();
	}

	public int getFunctionsCount() {
		return functionsCount;
	}

	/**
	 * Translates the user code written with the user library to a runtime
	 * compatible code.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param symbolTable
	 *            Symbol table for current class.
	 * @param listener
	 *            Compiler second pass listener that was used to walk on this
	 *            class' parse tree.
	 * @param lastIteratorCount
	 *            Contains the number of iterators counted so far.
	 */
	public void run(TokenStreamRewriter tokenStreamRewriter,
			Symbol symbolTable, CompilerSecondPassListener listener,
			int lastIteratorCount) throws CompilationException {
		ArrayList<Symbol> classSymbols = symbolTable
				.getSymbols(ClassSymbol.class);
		// Gets the class symbol table
		if (!classSymbols.isEmpty()) {
			ClassSymbol classSymbol = (ClassSymbol) classSymbols.get(0);
			// 1. Get iterators and set proper types (parallel or sequential)
			// depending on its code structure.
			ArrayList<UserLibraryData> iteratorsAndBinds = listener
					.getIteratorsAndBinds();
			this.setIteratorsTypes(iteratorsAndBinds);
			String packageName = listener.getPackageName();
			// 2. Perform input data binding
			ArrayList<InputBind> inputBinds = new ArrayList<>();
			for (Pair<UserLibraryVariableSymbol, CreatorSymbol> pair : this
					.getVariablesWithCreators(symbolTable)) {
				InputBind inputBind = this.replaceInputBind(
						tokenStreamRewriter, pair.left, pair.right,
						classSymbol.name, iteratorsAndBinds.size());
				inputBinds.add(inputBind);
			}
			// 3. Replace non-iterators and non-output bind method calls
			this.replaceMethodCalls(listener.getMethodCalls(),
					tokenStreamRewriter);
			// Translation step by step:
			// 4. Replace iterators
			List<Iterator> iterators = this.replaceIterators(
					tokenStreamRewriter, iteratorsAndBinds, classSymbol);
			// 5. Perform output data binding
			List<OutputBind> outputBinds = this.replaceOutputBinds(
					tokenStreamRewriter, iteratorsAndBinds, classSymbol.name);
			this.runtime.translateIteratorsAndBinds(packageName,
					classSymbol.name, iterators, inputBinds, outputBinds);
			if (listener.getUserLibraryDetected()) {
				// 6. Remove user library imports (if any)
				this.removeUserLibraryImports(tokenStreamRewriter,
						listener.getImportTokens());
				// 7. If user library classes were detected, we must insert
				// runtime imports
				this.insertRuntimeImports(tokenStreamRewriter, classSymbol,
						iteratorsAndBinds);
			}
			// 8. Initialize the runtime.
			StringBuffer initialization = new StringBuffer();
			initialization.append("\n"
					+ this.runtime.getInitializationString(packageName,
							classSymbol.name));
			tokenStreamRewriter.insertAfter(classSymbol.bodyAddress.start,
					initialization.toString());
			// After code translation, stores the output code in a Java file
			FileWriter.writeFile(classSymbol.name + ".java",
					this.commondDefinitions.getJavaDestinationFolder(
							this.outputDestinationFolder, packageName),
					tokenStreamRewriter.getText());
		}
	}

	/**
	 * Check each iterator and find out if they are parallel or sequential
	 * iterators. Parallel iterators must have ALL external variables final,
	 * whereas iterators that contains non-const variables will be compiled to
	 * sequential versions in the target runtime.
	 * 
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found.
	 */
	private void setIteratorsTypes(List<UserLibraryData> iteratorsAndBinds) {
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (userLibraryData instanceof Iterator) {
				Iterator iterator = (Iterator) userLibraryData;
				List<Variable> variables = iterator.getExternalVariables();
				Iterator.IteratorType iteratorType = IteratorType.Parallel;
				for (int i = 0; i < variables.size()
						&& iteratorType == IteratorType.Parallel; i++) {
					if (!variables.get(i).modifier.equals("final")) {
						SimpleLogger
								.warn("Iterator with non-final external variable in line "
										+ iterator.getStatementAddress().start
												.getLine()
										+ " will be translated to a sequential iterator in the target runtime.");
						iteratorType = IteratorType.Sequential;
					}
				}
				iterator.setType(iteratorType);
			}
		}
	}

	/**
	 * List all those user library variables that contains a creator in the
	 * scope.
	 * 
	 * @param classTable
	 *            Symbol table for current class.
	 * 
	 * @return List of pairs with user library variables and its creators.
	 */
	private List<Pair<UserLibraryVariableSymbol, CreatorSymbol>> getVariablesWithCreators(
			Symbol classTable) {
		ArrayList<Pair<UserLibraryVariableSymbol, CreatorSymbol>> variables = new ArrayList<>();
		// Get all creators' symbols
		ArrayList<Symbol> creators = classTable.getSymbols(CreatorSymbol.class);
		// And filter those creators that correspond to user library instances.
		for (Symbol symbol : creators) {
			CreatorSymbol creator = (CreatorSymbol) symbol;
			UserLibraryVariableSymbol variable = (UserLibraryVariableSymbol) creator.enclosingScope
					.getSymbolUnderScope(creator.attributedObjectName,
							UserLibraryVariableSymbol.class);
			if (variable != null) {
				if (UserLibraryClassFactory.create(variable.typeName) instanceof UserLibraryCollectionClass) {
					variables.add(new Pair<>(variable, creator));
				}
			}
		}
		return variables;
	}

	/**
	 * Perform memory binding for data input on the provided runtime.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param variableSymbol
	 *            Variable symbol for current input bind.
	 * @param creatorSymbol
	 *            Creator symbol for current input bind.
	 * @param className
	 *            Name of current class.
	 * @param functionNumber
	 *            Number that will be used associated to current input bind.
	 */
	private InputBind replaceInputBind(TokenStreamRewriter tokenStreamRewriter,
			VariableSymbol variableSymbol, CreatorSymbol creatorSymbol,
			String className, int functionNumber) throws CompilationException {
		// Creates a variable description to avoid unnecessary
		// coupling between the runtime definition and compiler
		// core.
		Variable variable = new Variable(variableSymbol.name,
				variableSymbol.typeName, variableSymbol.typeParameterName,
				variableSymbol.modifier);
		Parameter[] arguments = this
				.argumentsToVariableParameter(creatorSymbol.arguments);
		InputBind inputBind = new InputBind(variable, functionNumber, arguments);
		String inputBindDeclaration = this.runtime.getTranslator(
				inputBind.getVariable().typeName)
				.translateInputBindObjDeclaration(inputBind);
		String inputBindCreation = this.runtime.getTranslator(
				inputBind.getVariable().typeName)
				.translateInputBindObjCreation(className, inputBind);
		tokenStreamRewriter.insertBefore(variableSymbol.statementAddress.start,
				inputBindDeclaration);
		tokenStreamRewriter.insertAfter(creatorSymbol.statementAddress.stop,
				inputBindCreation);
		tokenStreamRewriter.delete(variableSymbol.statementAddress.start,
				variableSymbol.statementAddress.stop);
		if (!variableSymbol.statementAddress
				.equals(creatorSymbol.statementAddress)) {
			tokenStreamRewriter.delete(creatorSymbol.statementAddress.start,
					creatorSymbol.statementAddress.stop);
		}
		return inputBind;
	}

	/**
	 * Replace non-iterator and non-output bind method calls.
	 * 
	 * @param methodCalls
	 *            Set of method calls that must be replaced.
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 */
	private void replaceMethodCalls(Collection<MethodCall> methodCalls,
			TokenStreamRewriter tokenStreamRewriter)
			throws CompilationException {
		for (MethodCall methodCall : methodCalls) {
			tokenStreamRewriter.replace(methodCall.expressionAddress.start,
					methodCall.expressionAddress.stop,
					this.runtime.translateMethodCall(methodCall));
		}
	}

	/**
	 * Transforms a collection of arguments into an array of variable
	 * descriptors' objects.
	 * 
	 * @param arguments
	 *            Collection of arguments' symbols.
	 * @return Symbols' values converted to variable descriptors.
	 */
	private Parameter[] argumentsToVariableParameter(
			Collection<Symbol> arguments) {
		Object[] argumentsArray = arguments.toArray();
		Parameter[] ret = new Parameter[argumentsArray.length];
		for (int i = 0; i < ret.length; i++) {
			Symbol argument = (Symbol) argumentsArray[i];
			if (argument instanceof LiteralSymbol<?>) {
				LiteralSymbol<?> literal = (LiteralSymbol<?>) argument;
				String type = "";
				if (literal instanceof LiteralBooleanSymbol)
					type = "boolean";
				else if (literal instanceof LiteralCharacterSymbol)
					type = "char";
				else if (literal instanceof LiteralFloatingPointSymbol)
					type = "float";
				else if (literal instanceof LiteralIntegerSymbol)
					type = "int";
				else if (literal instanceof LiteralStringSymbol)
					type = "String";
				ret[i] = new Literal(literal.value.toString(), type);
			} else if (argument instanceof VariableSymbol) {
				VariableSymbol variable = (VariableSymbol) argument;
				ret[i] = new Variable(variable.name, variable.typeName,
						variable.typeParameterName, variable.modifier);
			} else if (argument instanceof ExpressionSymbol) {
				ExpressionSymbol expression = (ExpressionSymbol) argument;
				ret[i] = new Expression(expression.name);
			} else {
				// TODO Must throw an error in case argument is not literal nor
				// literal.
			}
		}
		return ret;
	}

	/**
	 * Replace iterators and initialize its runtime-equivalent functions.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found.
	 * @param classSymbol
	 *            Symbol table for current class.
	 * 
	 * @return List of iterators.
	 */
	private List<Iterator> replaceIterators(
			TokenStreamRewriter tokenStreamRewriter,
			Collection<UserLibraryData> iteratorsAndBinds,
			ClassSymbol classSymbol) throws CompilationException {
		this.functionsCount += iteratorsAndBinds.size();
		HashSet<Variable> variables = new HashSet<>();
		ArrayList<Iterator> iterators = new ArrayList<>();
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (userLibraryData instanceof Iterator) {
				Iterator iterator = (Iterator) userLibraryData;
				// 1. Iterators are translated on a single runtime call,
				// so they must be grouped on a single list.
				iterators.add(iterator);
				// 2. Replace iterator code
				String iteratorCall = this.runtime.getTranslator(
						iterator.getVariable().typeName).translateIteratorCall(
						classSymbol.name, iterator);
				tokenStreamRewriter.replace(
						iterator.getStatementAddress().start,
						iterator.getStatementAddress().stop, iteratorCall);
				// 3. Control user library variables in order to initialize
				// allocation and type variables for each of them.
				if (!variables.contains(iterator.getVariable())) {
					variables.add(iterator.getVariable());
				}
			}
		}
		return iterators;
	}

	/**
	 * Perform memory binding for data output on the provided runtime.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found.
	 * @param className
	 *            Name of current class.
	 * 
	 * @return List of output binds.
	 */
	private List<OutputBind> replaceOutputBinds(
			TokenStreamRewriter tokenStreamRewriter,
			Collection<UserLibraryData> iteratorsAndBinds, String className)
			throws CompilationException {
		ArrayList<OutputBind> outputBinds = new ArrayList<>();
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (userLibraryData instanceof OutputBind) {
				OutputBind outputBind = (OutputBind) userLibraryData;
				tokenStreamRewriter
						.replace(
								outputBind.statementAddress.start,
								outputBind.statementAddress.stop,
								this.runtime.getTranslator(
										outputBind.getVariable().typeName)
										.translateOutputBindCall(className,
												outputBind));
				outputBinds.add(outputBind);
			}
		}
		return outputBinds;
	}

	/**
	 * Remove user library imports.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param importTokens
	 *            Set of import tokens that must be removed.
	 */
	private void removeUserLibraryImports(
			TokenStreamRewriter tokenStreamRewriter,
			Collection<TokenAddress> importTokens) {
		for (TokenAddress importToken : importTokens) {
			tokenStreamRewriter.delete(importToken.start, importToken.stop);
		}
	}

	/**
	 * Insert necessary imports for the runtime usage.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param classTable
	 *            Symbol table for current class.
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found.
	 */
	private void insertRuntimeImports(TokenStreamRewriter tokenStreamRewriter,
			Symbol classTable, List<UserLibraryData> iteratorsAndBinds)
			throws CompilationException {
		tokenStreamRewriter.insertBefore(classTable.tokenAddress.start,
				this.runtime.getImports(iteratorsAndBinds));
	}
}
