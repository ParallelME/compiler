/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import br.ufmg.dcc.parallelme.compiler.runtime.*;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.*;
import br.ufmg.dcc.parallelme.compiler.symboltable.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryCollectionClass;
import br.ufmg.dcc.parallelme.compiler.util.Pair;

/**
 * Translates the user code written with the user library to a runtime
 * compatible code.
 * 
 * @author Wilson de Carvalho
 */
public class TranslatorSecondPass {
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
	public TranslatorSecondPass(RuntimeDefinition runtime,
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
			Symbol symbolTable, ParseTree tree, int lastIteratorCount) {
		// 1. Walk on the parse tree again
		ParseTreeWalker walker = new ParseTreeWalker();
		TranslatorSecondPassListener listener = new TranslatorSecondPassListener(
				tokenStreamRewriter.getTokenStream(), lastIteratorCount
						+ functionsCount);
		walker.walk(listener, tree);
		ArrayList<UserLibraryData> iteratorsAndBinds = listener
				.getIteratorsAndBinds();
		String packageName = listener.getPackageName();
		// 2. Perform input data binding
		for (Pair<UserLibraryVariableSymbol, CreatorSymbol> pair : this
				.getVariablesWithCreators(symbolTable)) {
			this.bindRuntimeInputData(tokenStreamRewriter, pair.left,
					pair.right, iteratorsAndBinds, packageName,
					iteratorsAndBinds.size());
		}
		// 3. Replace iterators and perform output data binding
		ArrayList<Symbol> classSymbols = symbolTable
				.getSymbols(ClassSymbol.class);
		// Gets the class symbol table
		if (!classSymbols.isEmpty()) {
			ClassSymbol classSymbol = (ClassSymbol) classSymbols.get(0);
			// Translation step by step:
			this.replaceIterators(tokenStreamRewriter, iteratorsAndBinds,
					packageName, lastIteratorCount, classSymbol);
			this.bindRuntimeOutputData(tokenStreamRewriter, iteratorsAndBinds,
					packageName);
			// After code translation, stores the output code in a Java file
			this.writeOutputFile(classSymbol.name + ".java",
					tokenStreamRewriter.getText());
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
	private void bindRuntimeInputData(TokenStreamRewriter tokenStreamRewriter,
			VariableSymbol variableSymbol, CreatorSymbol creatorSymbol,
			ArrayList<UserLibraryData> iteratorsAndBinds, String packageName,
			int functionNumber) {
		// Creates a variable description to avoid unnecessary
		// coupling between the runtime definition and compiler
		// core.
		Variable variable = new Variable(variableSymbol.name,
				variableSymbol.typeName, variableSymbol.typeParameterName);
		Parameter[] arguments = this
				.argumentsToVariableParameter(creatorSymbol.arguments);
		InputBind inputBind = new InputBind(variable, functionNumber, arguments);
		String inputBindDeclaration = this.runtime.declareAllocation(inputBind);
		String inputBindCreation = this.runtime.createAllocation(inputBind);
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
		String inputBindFunction = this.runtime
				.createAllocationFunction(inputBind);
		if (!inputBindFunction.isEmpty()) {
			String functionCode = this.runtime.getCFunctionHeader(packageName)
					+ "\n" + inputBindFunction;
			String fileName = this.runtime
					.getFunctionName(inputBind.sequentialNumber)
					+ "."
					+ this.runtime.getCFileExtension();
			this.writeOutputFile(fileName, functionCode);
			iteratorsAndBinds.add(inputBind);
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
						variable.typeParameterName);
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
	private void replaceIterators(TokenStreamRewriter tokenStreamRewriter,
			Collection<UserLibraryData> iteratorsAndBinds, String packageName,
			int lastFunctionCount, ClassSymbol classSymbol) {
		this.functionsCount += iteratorsAndBinds.size();
		HashSet<Variable> variables = new HashSet<>();
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (userLibraryData instanceof Iterator) {
				Iterator iterator = (Iterator) userLibraryData;
				// 1. Replace iterator code
				String iteratorCall = this.runtime.getIterator(
						iterator.getVariable(), iterator.sequentialNumber);
				tokenStreamRewriter.replace(
						iterator.getStatementAddress().start,
						iterator.getStatementAddress().stop, iteratorCall);
				// 2. Call Java2C to translate C code
				this.translateCCode(packageName, (Iterator) iterator);
				// 3. Control user library variables in order to initialize
				// allocation and type variables for each of them.
				if (!variables.contains(iterator.getVariable())) {
					variables.add(iterator.getVariable());
				}
			}
		}
		StringBuffer initialization = new StringBuffer();
		initialization.append("\n" + this.runtime.getInitializationString());
		initialization.append("\n"
				+ this.runtime.getFunctionInitializationString(
						lastFunctionCount, iteratorsAndBinds.size()));
		tokenStreamRewriter.insertAfter(classSymbol.bodyAddress.start,
				initialization.toString());

	}

	/**
	 * Perform memory binding for data output on the provided runtime.
	 */
	private void bindRuntimeOutputData(TokenStreamRewriter tokenStreamRewriter,
			Collection<UserLibraryData> outputBinds, String packageName) {
		for (UserLibraryData userLibraryData : outputBinds) {
			if (userLibraryData instanceof OutputBind) {
				OutputBind outputBind = (OutputBind) userLibraryData;
				tokenStreamRewriter.replace(
						outputBind.getStatementAddress().start,
						outputBind.getStatementAddress().stop,
						this.runtime.getAllocationData(outputBind));
				String outputBindFunction = this.runtime
						.getAllocationDataFunction(outputBind);
				if (!outputBindFunction.isEmpty()) {
					String functionCode = this.runtime
							.getCFunctionHeader(packageName)
							+ "\n"
							+ outputBindFunction;
					String fileName = this.runtime
							.getFunctionName(outputBind.sequentialNumber)
							+ "."
							+ this.runtime.getCFileExtension();
					this.writeOutputFile(fileName, functionCode);
				}
			}
		}
	}

	/**
	 * Create a C files for the iterators provided.
	 */
	private void translateCCode(String packageName, Iterator iterator) {
		String fileName = this.runtime
				.getFunctionName(iterator.sequentialNumber);
		this.writeOutputFile(fileName + "." + this.runtime.getCFileExtension(),
				this.runtime.getCFunctionHeader(packageName) + "\n"
						+ this.runtime.translateIteratorCode(iterator));
	}

	/**
	 * Writes files on the output folder.
	 */
	private void writeOutputFile(String fileName, String fileContent) {
		PrintWriter writer = null;
		try {
			try {
				writer = new PrintWriter(this.outputDestinationFolder + "\\"
						+ fileName, "UTF-8");
				writer.print(fileContent);
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} finally {
			if (writer != null)
				writer.close();
		}
	}

}
