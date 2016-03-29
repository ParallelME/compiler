/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;

import br.ufmg.dcc.parallelme.compiler.runtime.*;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Iterator.IteratorType;
import br.ufmg.dcc.parallelme.compiler.symboltable.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryCollectionClass;
import br.ufmg.dcc.parallelme.compiler.util.FileWriter;
import br.ufmg.dcc.parallelme.compiler.util.Pair;

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

	/**
	 * 
	 * @param runtime
	 *            The selected runtime for the output code.
	 * @param outputDestinationFolder
	 *            Output destination folder for compiled files.
	 */
	public CompilerCodeTranslator(RuntimeDefinition runtime,
			String outputDestinationFolder) {
		this.runtime = runtime;
		this.outputDestinationFolder = outputDestinationFolder;
	}

	public int getFunctionsCount() {
		return functionsCount;
	}

	/**
	 * Translates the user code written with the user library to a runtime
	 * compatible code.
	 */
	public void run(TokenStreamRewriter tokenStreamRewriter,
			Symbol symbolTable, CompilerSecondPassListener listener,
			int lastIteratorCount) {
		ArrayList<Symbol> classSymbols = symbolTable
				.getSymbols(ClassSymbol.class);
		// Gets the class symbol table
		if (!classSymbols.isEmpty()) {
			ClassSymbol classSymbol = (ClassSymbol) classSymbols.get(0);
			// 1. Get iterators and set proper types (parallel or sequential)
			// depending on its code structure.
			ArrayList<UserLibraryData> iteratorsAndBinds = listener
					.getIteratorsAndBinds();
			this.setIteratorsTypes(iteratorsAndBinds,
					tokenStreamRewriter.getTokenStream());
			String packageName = listener.getPackageName();
			// 2. Perform input data binding
			ArrayList<InputBind> inputBinds = new ArrayList<>();
			for (Pair<UserLibraryVariableSymbol, CreatorSymbol> pair : this
					.getVariablesWithCreators(symbolTable)) {
				InputBind inputBind = this.replaceInputBind(
						tokenStreamRewriter, pair.left, pair.right,
						iteratorsAndBinds, classSymbol.name,
						iteratorsAndBinds.size());
				inputBinds.add(inputBind);
			}
			// 3. Replace non-iterators and non-output bind method calls
			this.replaceMethodCalls(listener.getMethodCalls(),
					tokenStreamRewriter);
			// Translation step by step:
			// 4. Replace iterators
			List<Iterator> iterators = this.replaceIterators(
					tokenStreamRewriter, iteratorsAndBinds, packageName,
					lastIteratorCount, classSymbol);
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
			// 8. Initialize the runtime using all input binds, iterators and
			// output bind information found.
			StringBuffer initialization = new StringBuffer();
			initialization.append("\n"
					+ this.runtime.getInitializationString(packageName,
							classSymbol.name, inputBinds, iterators,
							outputBinds));
			tokenStreamRewriter.insertAfter(classSymbol.bodyAddress.start,
					initialization.toString());
			// After code translation, stores the output code in a Java file
			FileWriter
					.writeFile(classSymbol.name + ".java",
							this.outputDestinationFolder,
							tokenStreamRewriter.getText());
		}
	}

	/**
	 * Check each iterator and find out if they are parallel or sequential
	 * iterators. Parallel iterators must have ALL external variables final,
	 * whereas iterators that contains non-const variables will be compiled to
	 * sequential versions in the target runtime.
	 */
	private void setIteratorsTypes(
			ArrayList<UserLibraryData> iteratorsAndBinds,
			TokenStream tokenStream) {
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
	 * @return
	 */
	private ArrayList<Pair<UserLibraryVariableSymbol, CreatorSymbol>> getVariablesWithCreators(
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
	 */
	private InputBind replaceInputBind(TokenStreamRewriter tokenStreamRewriter,
			VariableSymbol variableSymbol, CreatorSymbol creatorSymbol,
			ArrayList<UserLibraryData> iteratorsAndBinds, String className,
			int functionNumber) {
		// Creates a variable description to avoid unnecessary
		// coupling between the runtime definition and compiler
		// core.
		Variable variable = new Variable(variableSymbol.name,
				variableSymbol.typeName, variableSymbol.typeParameterName,
				variableSymbol.modifier);
		Parameter[] arguments = this
				.argumentsToVariableParameter(creatorSymbol.arguments);
		InputBind inputBind = new InputBind(variable, functionNumber, arguments);
		String inputBindDeclaration = this.runtime.declareAllocation(inputBind);
		String inputBindCreation = this.runtime.createAllocation(className,
				inputBind);
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
	 */
	private void replaceMethodCalls(Collection<MethodCall> methodCalls,
			TokenStreamRewriter tokenStreamRewriter) {
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
		Parameter[] ret = new Variable[argumentsArray.length];
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
			} else {
				// TODO Must throw an error in case argument is not literal nor
				// literal.
			}
		}
		return ret;
	}

	/**
	 * Replace iterators and initialize its runtime-equivalent functions.
	 */
	private List<Iterator> replaceIterators(
			TokenStreamRewriter tokenStreamRewriter,
			Collection<UserLibraryData> iteratorsAndBinds, String packageName,
			int lastFunctionCount, ClassSymbol classSymbol) {
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
				String iteratorCall = this.runtime.getIteratorCall(
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
	 */
	private List<OutputBind> replaceOutputBinds(
			TokenStreamRewriter tokenStreamRewriter,
			Collection<UserLibraryData> iteratorsAndBinds, String className) {
		ArrayList<OutputBind> outputBinds = new ArrayList<>();
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (userLibraryData instanceof OutputBind) {
				OutputBind outputBind = (OutputBind) userLibraryData;
				tokenStreamRewriter.replace(
						outputBind.getExpressionAddress().start,
						outputBind.getExpressionAddress().stop,
						this.runtime.getAllocationData(className, outputBind));
				outputBinds.add(outputBind);
			}
		}
		return outputBinds;
	}

	/**
	 * Remove user library imports.
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
	 */
	private void insertRuntimeImports(TokenStreamRewriter tokenStreamRewriter,
			Symbol classTable, ArrayList<UserLibraryData> iteratorsAndBinds) {
		tokenStreamRewriter.insertBefore(classTable.tokenAddress.start,
				this.runtime.getImports(iteratorsAndBinds));
	}
}
