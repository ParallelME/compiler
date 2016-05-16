/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.renderscript.RenderScriptRuntimeDefinition;
import org.parallelme.compiler.runtime.ParallelMERuntimeDefinition;
import org.parallelme.compiler.symboltable.*;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;
import org.parallelme.compiler.userlibrary.UserLibraryClass;
import org.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import org.parallelme.compiler.userlibrary.UserLibraryCollectionClass;
import org.parallelme.compiler.util.FileWriter;
import org.parallelme.compiler.util.Pair;
import org.stringtemplate.v4.ST;

/**
 * Translates the user code written with the user library to a runtime
 * compatible code.
 * 
 * @author Wilson de Carvalho
 */
public class CompilerCodeTranslator {
	private final String outputDestinationFolder;
	private final RuntimeCommonDefinitions commonDefinitions;
	private final RuntimeDefinition rsRuntime;
	private final RuntimeDefinition pmRuntime;
	private final String templateJavaInterface = "<introductoryMsg>\n"
			+ "package <packageName>;\n\n"
			+ "public interface <interfaceName> {\n"
			+ "\tpublic boolean isValid();\n\n"
			+ "\t<methods:{var|<var.signature>;}; separator=\"\\n\\n\">"
			+ "\n}\n";
	private final String templateJavaClass = "<introductoryMsg>\n"
			+ "package <packageName>;\n\n"
			+ "<imports>\n"
			+ "public class <className> implements <interfaceName> {\n"
			+ "\t<classDeclarations:{var|<var.line>\n}>\n"
			+ "\tpublic boolean isValid() {\n\t\t<isValidBody>\n\t\\}\n\n"
			+ "\t<methods:{var|<var.signature> {\n\t<var.body>\n\\}}; separator=\"\\n\\n\">"
			+ "\n\\}\n";

	/**
	 * Base constructor.
	 * 
	 * @param outputDestinationFolder
	 *            Output destination folder for compiled files.
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 */
	public CompilerCodeTranslator(String outputDestinationFolder,
			CTranslator cTranslator) {
		this.outputDestinationFolder = outputDestinationFolder;
		this.commonDefinitions = new RuntimeCommonDefinitions();
		this.rsRuntime = new RenderScriptRuntimeDefinition(cTranslator,
				outputDestinationFolder);
		this.pmRuntime = new ParallelMERuntimeDefinition(cTranslator,
				outputDestinationFolder);
	}

	/**
	 * Translates the user code written with the user library to a runtime
	 * compatible code.
	 * 
	 * @param symbolTable
	 *            Symbol table for current class.
	 * @param listener
	 *            Compiler second pass listener that was used to walk on this
	 *            class' parse tree.
	 */
	public void run(Symbol symbolTable, CompilerSecondPassListener listener,
			TokenStreamRewriter tokenStreamRewriter)
			throws CompilationException {
		ArrayList<Symbol> classSymbols = symbolTable
				.getSymbols(ClassSymbol.class);
		// Gets the class symbol table
		if (!classSymbols.isEmpty()) {
			// 1. Get iterators and set proper types (parallel or sequential)
			// depending on its code structure.
			ArrayList<UserLibraryData> iteratorsAndBinds = listener
					.getIteratorsAndBinds();
			this.setIteratorsTypes(iteratorsAndBinds);
			String packageName = listener.getPackageName();
			ClassSymbol classSymbol = (ClassSymbol) symbolTable.getSymbols(
					ClassSymbol.class).get(0);
			List<InputBind> inputBinds = this.getInputBinds(symbolTable);
			Pair<List<Iterator>, List<OutputBind>> iteratorsAndOutputBinds = this
					.getIteratorsAndOutputBinds(iteratorsAndBinds);
			Collection<MethodCall> methodCalls = listener.getMethodCalls();
			String className = classSymbol.name;
			// 2. Creates the java interface tha will be used to implement each
			// runtime code.
			this.createJavaWrapperInterface(packageName, className,
					iteratorsAndOutputBinds.left, inputBinds,
					iteratorsAndOutputBinds.right, methodCalls);
			// 3. Creates RenderScript implentation
			this.createJavaWrapperImplementation(packageName, className,
					iteratorsAndOutputBinds.left, inputBinds,
					iteratorsAndOutputBinds.right, methodCalls, this.rsRuntime);
			// 4. Creates ParallelME implentation
			this.createJavaWrapperImplementation(packageName, className,
					iteratorsAndOutputBinds.left, inputBinds,
					iteratorsAndOutputBinds.right, methodCalls, this.pmRuntime);
			// 5. Translate the user code, calling the runtime wrapper
			this.translateUserCode(packageName, className,
					iteratorsAndOutputBinds.left, inputBinds,
					iteratorsAndOutputBinds.right, methodCalls,
					listener.getImportTokens(), tokenStreamRewriter);
			// 6. Export internal library files for each target runtime
			try {
				this.rsRuntime.exportInternalLibrary("",
						outputDestinationFolder);
				this.pmRuntime.exportInternalLibrary("",
						outputDestinationFolder);
			} catch (IOException e) {
				throw new CompilationException(
						"Error exporting internal library files: "
								+ e.getMessage());
			}
		}
	}

	/**
	 * Creates a Java interface that will be used as a wrapper for all runtime
	 * calls.
	 * 
	 * @param packageName
	 *            Name of the package of which current data (class, iterators
	 *            and binds) belong.
	 * @param className
	 *            Name of the class of which current data (iterators and binds)
	 *            belong.
	 * @param iterators
	 *            Iterator that must be translated.
	 * @param inputBinds
	 *            Input binds that must be translated.
	 * @param outputBinds
	 *            Output binds that must be translated.
	 * @param methodCalls
	 *            Set of method calls that must be replaced.
	 */
	private void createJavaWrapperInterface(String packageName,
			String className, List<Iterator> iterators,
			List<InputBind> inputBinds, List<OutputBind> outputBinds,
			Collection<MethodCall> methodCalls) {
		String interfaceName = this.commonDefinitions
				.getJavaWrapperInterfaceName(className);
		ST st = new ST(templateJavaInterface);
		st.add("introductoryMsg", this.commonDefinitions.getHeaderComment());
		st.add("packageName", packageName);
		st.add("interfaceName", interfaceName);
		for (InputBind inputBind : inputBinds) {
			st.addAggr("methods.{signature}",
					this.commonDefinitions.createJavaMethodSignature(inputBind));
		}
		for (Iterator iterator : iterators) {
			st.addAggr("methods.{signature}",
					this.commonDefinitions.createJavaMethodSignature(iterator));
		}
		for (OutputBind outputBind : outputBinds) {
			st.addAggr("methods.{signature}", this.commonDefinitions
					.createJavaMethodSignature(outputBind));
		}
		for (MethodCall methodCall : methodCalls) {
			st.addAggr("methods.{signature}", this.commonDefinitions
					.createJavaMethodSignature(methodCall));
		}
		FileWriter.writeFile(interfaceName + ".java", this.commonDefinitions
				.getJavaDestinationFolder(this.outputDestinationFolder,
						packageName), st.render());
	}

	/**
	 * Creates a Java implementation class for the interface wrapper created
	 * previously for a given runtime.
	 * 
	 * @param packageName
	 *            Name of the package of which current data (class, iterators
	 *            and binds) belong.
	 * @param className
	 *            Name of the class of which current data (iterators and binds)
	 *            belong.
	 * @param iterators
	 *            Iterator that must be translated.
	 * @param inputBinds
	 *            Input binds that must be translated.
	 * @param outputBinds
	 *            Output binds that must be translated.
	 * @param methodCalls
	 *            Set of method calls that must be replaced.
	 * @param targetRuntime
	 *            Target runtime that will be used to create the class
	 *            implementation.
	 */
	private void createJavaWrapperImplementation(String packageName,
			String className, List<Iterator> iterators,
			List<InputBind> inputBinds, List<OutputBind> outputBinds,
			Collection<MethodCall> methodCalls, RuntimeDefinition targetRuntime)
			throws CompilationException {
		String interfaceName = this.commonDefinitions
				.getJavaWrapperInterfaceName(className);
		String javaClassName = this.commonDefinitions.getJavaWrapperClassName(
				className, targetRuntime.getTargetRuntime());
		ST st = new ST(templateJavaClass);
		st.add("introductoryMsg", this.commonDefinitions.getHeaderComment());
		st.add("packageName", packageName);
		st.add("interfaceName", interfaceName);
		st.add("className", javaClassName);
		List<UserLibraryData> iteratorsAndBinds = new ArrayList<>();
		iteratorsAndBinds.addAll(inputBinds);
		iteratorsAndBinds.addAll(iterators);
		iteratorsAndBinds.addAll(outputBinds);
		st.add("imports", targetRuntime.getImports(iteratorsAndBinds));
		st.add("classDeclarations", null);
		for (InputBind inputBind : inputBinds) {
			UserLibraryTranslatorDefinition translator = targetRuntime
					.getTranslator(inputBind.variable.typeName);
			st.addAggr(
					"classDeclarations.{line}",
					"private "
							+ translator
									.translateInputBindObjDeclaration(inputBind));
			String methodSignature = this.commonDefinitions
					.createJavaMethodSignature(inputBind);
			String body = translator.translateInputBindObjCreation(
					javaClassName, inputBind);
			st.addAggr("methods.{signature, body}", methodSignature, body);

		}
		for (String line : targetRuntime.getIsValidBody()) {
			st.add("isValidBody", line);
		}
		for (String line : targetRuntime.getInitializationString(packageName,
				javaClassName)) {
			st.addAggr("classDeclarations.{line}", line);
		}
		for (Iterator iterator : iterators) {
			String methodSignature = this.commonDefinitions
					.createJavaMethodSignature(iterator);
			String body = targetRuntime.getTranslator(
					iterator.variable.typeName).translateIteratorCall(
					javaClassName, iterator);
			st.addAggr("methods.{signature, body}", methodSignature, body);
		}
		for (OutputBind outputBind : outputBinds) {
			String methodSignature = this.commonDefinitions
					.createJavaMethodSignature(outputBind);
			String body = targetRuntime.getTranslator(
					outputBind.variable.typeName).translateOutputBindCall(
					javaClassName, outputBind);
			st.addAggr("methods.{signature, body}", methodSignature, body);
		}
		for (MethodCall methodCall : methodCalls) {
			String methodSignature = this.commonDefinitions
					.createJavaMethodSignature(methodCall);
			String body = targetRuntime.getTranslator(
					methodCall.variable.typeName).translateMethodCall(
					javaClassName, methodCall);
			st.addAggr("methods.{signature, body}", methodSignature, body);
		}
		FileWriter.writeFile(javaClassName + ".java", this.commonDefinitions
				.getJavaDestinationFolder(this.outputDestinationFolder,
						packageName), st.render());
	}

	/**
	 * Translates the original user code, replacing user library references by
	 * runtime wrapper references.
	 * 
	 * @param packageName
	 *            Name of the package of which current data (class, iterators
	 *            and binds) belong.
	 * @param className
	 *            Name of the class of which current data (iterators and binds)
	 *            belong.
	 * @param iterators
	 *            Iterator that must be translated.
	 * @param inputBinds
	 *            Input binds that must be translated.
	 * @param outputBinds
	 *            Output binds that must be translated.
	 * @param methodCalls
	 *            Set of method calls that must be replaced.
	 * @param importTokens
	 *            Set of import tokens that must be removed.
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 */
	private void translateUserCode(String packageName, String className,
			List<Iterator> iterators, List<InputBind> inputBinds,
			List<OutputBind> outputBinds, Collection<MethodCall> methodCalls,
			Collection<TokenAddress> importTokens,
			TokenStreamRewriter tokenStreamRewriter) {
		this.removeUserLibraryImports(tokenStreamRewriter, importTokens);
		this.translateInputBinds(tokenStreamRewriter, className, inputBinds);
		this.translateIterators(tokenStreamRewriter, iterators);
		this.translateOutputBinds(tokenStreamRewriter, outputBinds);
		this.translateMethodCalls(tokenStreamRewriter, methodCalls);
		FileWriter.writeFile(className + ".java", this.commonDefinitions
				.getJavaDestinationFolder(this.outputDestinationFolder,
						packageName), tokenStreamRewriter.getText());
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
	 * Translate input binds in Java user code to call ParallelME runtime
	 * wrappers.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param className
	 *            Name of the class of which current data (iterators and binds)
	 *            belong.
	 * @param inputBinds
	 *            Input binds that must be translated.
	 */
	private void translateInputBinds(TokenStreamRewriter tokenStreamRewriter,
			String className, List<InputBind> inputBinds) {
		// There should be only a single declaration per class, where it will
		// declare ParallelME
		boolean parallelMEDeclared = false;
		String objectName = this.commonDefinitions.getParallelMEObjectName();
		for (InputBind inputBind : inputBinds) {
			if (!parallelMEDeclared) {
				String interfaceName = this.commonDefinitions
						.getJavaWrapperInterfaceName(className);
				tokenStreamRewriter.replace(
						inputBind.declarationStatementAddress.start,
						inputBind.declarationStatementAddress.stop,
						interfaceName + " " + objectName + ";");
				parallelMEDeclared = true;
			} else {
				tokenStreamRewriter.replace(
						inputBind.declarationStatementAddress.start,
						inputBind.declarationStatementAddress.stop, "");
			}
			tokenStreamRewriter
					.replace(
							inputBind.creationStatementAddress.start,
							inputBind.creationStatementAddress.stop,
							objectName
									+ "."
									+ this.commonDefinitions
											.getInputBindName(inputBind)
									+ "("
									+ this.commonDefinitions
											.toCommaSeparatedString(inputBind.parameters)
									+ ");");
		}
	}

	/**
	 * Translate iterators in Java user code to call ParallelME runtime
	 * wrappers.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param iterators
	 *            Iterator that must be translated.
	 */
	private void translateIterators(TokenStreamRewriter tokenStreamRewriter,
			List<Iterator> iterators) {
		String objectName = this.commonDefinitions.getParallelMEObjectName();
		for (Iterator iterator : iterators) {
			String translatedStatement;
			// Sequential iterators must create arrays to store variables
			if (iterator.getType() == IteratorType.Sequential) {
				String templateSequentialIterator = "<declParams:{var|<var.type>[] <var.arrName> = new <var.type>[1];\n"
						+ "<var.arrName>[0] = <var.varName>;\n}>"
						+ "<paralleMEObject>.<iteratorName>(<params:{var|<var.name>}; separator=\", \">);"
						+ "\n<recoverParams:{var|<var.varName> = <var.arrName>[0];}>";
				ST st = new ST(templateSequentialIterator);
				st.add("paralleMEObject", objectName);
				st.add("iteratorName",
						this.commonDefinitions.getIteratorName(iterator));
				for (Variable variable : iterator.getExternalVariables()) {
					if (!variable.isFinal()) {
						String arrName = this.commonDefinitions.getPrefix()
								+ variable.name;
						st.addAggr("declParams.{type, arrName, varName}",
								variable.typeName, arrName, variable.name);
						st.addAggr("recoverParams.{arrName, varName}", arrName,
								variable.name);
						st.addAggr("params.{name}", arrName);
					} else {
						st.addAggr("params.{name}", variable.name);
					}
				}
				translatedStatement = st.render();
			} else {
				translatedStatement = objectName
						+ "."
						+ this.commonDefinitions.getIteratorName(iterator)
						+ "("
						+ this.commonDefinitions
								.toCommaSeparatedString(iterator
										.getExternalVariables()) + ");";
			}
			tokenStreamRewriter.replace(iterator.statementAddress.start,
					iterator.statementAddress.stop, translatedStatement);
		}
	}

	/**
	 * Translate output binds in Java user code to call ParallelME runtime
	 * wrappers.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param outputBinds
	 *            Output binds that must be translated.
	 */
	private void translateOutputBinds(TokenStreamRewriter tokenStreamRewriter,
			List<OutputBind> outputBinds) {
		String objectName = this.commonDefinitions.getParallelMEObjectName();
		for (OutputBind outputBind : outputBinds) {
			String translatedStatement;
			if (outputBind.isObjectDeclaration) {
				translatedStatement = outputBind.destinationObject.typeName
						+ " " + outputBind.destinationObject.name + ";\n"
						+ objectName + "."
						+ this.commonDefinitions.getOutputBindName(outputBind)
						+ "(" + outputBind.destinationObject.name + ");";
			} else {
				translatedStatement = objectName + "."
						+ this.commonDefinitions.getOutputBindName(outputBind)
						+ "(" + outputBind.destinationObject.name + ");";

			}
			tokenStreamRewriter.replace(outputBind.statementAddress.start,
					outputBind.statementAddress.stop, translatedStatement);
		}
	}

	/**
	 * Translate non-iterator and non-output bind method calls.
	 * 
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param methodCalls
	 *            Set of method calls that must be replaced.
	 */
	private void translateMethodCalls(TokenStreamRewriter tokenStreamRewriter,
			Collection<MethodCall> methodCalls) {
		String objectName = this.commonDefinitions.getParallelMEObjectName();
		for (MethodCall methodCall : methodCalls) {
			tokenStreamRewriter.replace(
					methodCall.expressionAddress.start,
					methodCall.expressionAddress.stop,
					objectName
							+ "."
							+ this.commonDefinitions
									.getMethodCallName(methodCall) + "()");
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
				Variable[] variables = iterator.getExternalVariables();
				Iterator.IteratorType iteratorType = IteratorType.Parallel;
				for (int i = 0; i < variables.length
						&& iteratorType == IteratorType.Parallel; i++) {
					if (!variables[i].isFinal()) {
						SimpleLogger
								.warn("Iterator with non-final external variable in line "
										+ iterator.statementAddress.start
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
	 * Look for all input binds in a given symbol table.
	 * 
	 * @param symbolTable
	 *            Symbol table.
	 */
	private List<InputBind> getInputBinds(Symbol symbolTable)
			throws CompilationException {
		ArrayList<InputBind> ret = new ArrayList<>();
		int inputBindCount = 0;
		for (Pair<UserLibraryVariableSymbol, CreatorSymbol> pair : this
				.getVariablesWithCreators(symbolTable)) {
			VariableSymbol variableSymbol = (VariableSymbol) pair.left;
			Variable variable = new Variable(variableSymbol.name,
					variableSymbol.typeName, variableSymbol.typeParameterName,
					variableSymbol.modifier, variableSymbol.identifier);
			Parameter[] arguments = this
					.argumentsToVariableParameter(pair.right.arguments);
			ret.add(new InputBind(variable, ++inputBindCount, arguments,
					pair.left.statementAddress, pair.right.statementAddress));
		}
		return ret;
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
						variable.typeParameterName, variable.modifier,
						variable.identifier);
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
	 * Lists all iterators in a iterators and binds lists.
	 * 
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found.
	 * 
	 * @return List of iterators found.
	 */
	private Pair<List<Iterator>, List<OutputBind>> getIteratorsAndOutputBinds(
			Collection<UserLibraryData> iteratorsAndBinds) {
		ArrayList<Iterator> iterators = new ArrayList<>();
		ArrayList<OutputBind> outputBinds = new ArrayList<>();
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (userLibraryData instanceof Iterator) {
				iterators.add((Iterator) userLibraryData);
			} else if (userLibraryData instanceof OutputBind) {
				outputBinds.add((OutputBind) userLibraryData);
			}
		}
		return new Pair<List<Iterator>, List<OutputBind>>(iterators,
				outputBinds);
	}
}
