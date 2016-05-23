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
import java.util.TreeSet;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.parallelme.compiler.RuntimeDefinition.TargetRuntime;
import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.intermediate.*;
import org.parallelme.compiler.intermediate.Iterator.IteratorType;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.renderscript.RenderScriptRuntimeDefinition;
import org.parallelme.compiler.runtime.ParallelMERuntimeDefinition;
import org.parallelme.compiler.symboltable.*;
import org.parallelme.compiler.translation.CTranslator;
import org.parallelme.compiler.translation.userlibrary.UserLibraryTranslatorDefinition;
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
	private final RuntimeDefinition rsRuntime;
	private final RuntimeDefinition pmRuntime;
	private final static String templateJavaInterface = "<introductoryMsg>\n"
			+ "package <packageName>;\n\n"
			+ "<imports:{var|import <var.statement>;\n}>\n"
			+ "public interface <interfaceName> {\n"
			+ "\tpublic boolean isValid();\n\n"
			+ "\t<methods:{var|<var.signature>;}; separator=\"\\n\\n\">"
			+ "\n}\n";
	private final static String templateJavaClass = "<introductoryMsg>\n"
			+ "package <packageName>;\n\n"
			+ "<imports:{var|import <var.statement>;\n}>\n"
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
			String packageName = listener.getPackageName();
			ClassSymbol classSymbol = (ClassSymbol) symbolTable.getSymbols(
					ClassSymbol.class).get(0);
			IteratorsAndBinds iteratorsAndBinds = this.getIteratorsAndBinds(
					listener.getIteratorsAndBinds(), classSymbol);
			List<MethodCall> methodCalls = listener.getMethodCalls();
			String className = classSymbol.name;
			// 2. Creates the java interface that will be used to implement each
			// runtime code.
			this.createJavaWrapperInterface(packageName, className,
					iteratorsAndBinds, methodCalls);
			// 3. Translate code to RenderScript
			this.runtimeSpecificTranslation(packageName, className,
					iteratorsAndBinds, methodCalls, this.rsRuntime);
			// 4. Translate code to ParallelME runtime
			this.runtimeSpecificTranslation(packageName, className,
					iteratorsAndBinds, methodCalls, this.pmRuntime);
			// 5. Translate the user code, calling the runtime wrapper
			this.translateUserCode(packageName, className, classSymbol,
					iteratorsAndBinds, methodCalls, listener.getImportTokens(),
					tokenStreamRewriter);
		}
	}

	/**
	 * Perform runtime-specific translation.
	 * 
	 * @param packageName
	 *            Name of the package of which current data (class, iterators
	 *            and binds) belong.
	 * @param className
	 *            Name of the class of which current data (iterators and binds)
	 *            belong.
	 * @param iteratorsAndBinds
	 *            Container with iterators and binds.
	 * @param methodCalls
	 *            List of method calls that must be replaced.
	 * @throws CompilationException
	 */
	private void runtimeSpecificTranslation(String packageName,
			String className, IteratorsAndBinds iteratorsAndBinds,
			List<MethodCall> methodCalls, RuntimeDefinition targetRuntime)
			throws CompilationException {
		// 1. Creates Java wrapper implementation for interface created
		this.createJavaWrapperImplementation(packageName, className,
				iteratorsAndBinds, methodCalls, targetRuntime);
		// 2. Translate user code to C code compatible with the target runtime
		targetRuntime.translateIteratorsAndBinds(packageName, className,
				iteratorsAndBinds);
		// 3. Export internal library files for each target runtime
		try {
			targetRuntime.exportInternalLibrary("", outputDestinationFolder);
		} catch (IOException e) {
			throw new CompilationException(
					"Error exporting internal library files: " + e.getMessage());
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
	 * @param iteratorsAndBinds
	 *            Container with iterators and binds.
	 * @param methodCalls
	 *            List of method calls that must be replaced.
	 * @throws CompilationException
	 */
	private void createJavaWrapperInterface(String packageName,
			String className, IteratorsAndBinds iteratorsAndBinds,
			List<MethodCall> methodCalls) throws CompilationException {
		String interfaceName = RuntimeCommonDefinitions.getInstance()
				.getJavaWrapperInterfaceName(className);
		ST st = new ST(templateJavaInterface);
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		st.add("packageName", packageName);
		st.add("interfaceName", interfaceName);
		Set<String> userLibraryClasses = new HashSet<String>();
		for (InputBind inputBind : iteratorsAndBinds.inputBinds) {
			st.addAggr("methods.{signature}", RuntimeCommonDefinitions
					.getInstance().createJavaMethodSignature(inputBind));
			userLibraryClasses.add(inputBind.variable.typeName);
		}
		for (Iterator iterator : iteratorsAndBinds.iterators) {
			st.addAggr("methods.{signature}", RuntimeCommonDefinitions
					.getInstance().createJavaMethodSignature(iterator));
			userLibraryClasses.add(iterator.variable.typeName);
		}
		for (OutputBind outputBind : iteratorsAndBinds.outputBinds) {
			st.addAggr("methods.{signature}", RuntimeCommonDefinitions
					.getInstance().createJavaMethodSignature(outputBind));
			userLibraryClasses.add(outputBind.variable.typeName);
		}
		for (MethodCall methodCall : methodCalls) {
			st.addAggr("methods.{signature}", RuntimeCommonDefinitions
					.getInstance().createJavaMethodSignature(methodCall));
			userLibraryClasses.add(methodCall.variable.typeName);
		}
		Set<String> imports = this.getImports(userLibraryClasses,
				this.rsRuntime, true);
		imports.addAll(this
				.getImports(userLibraryClasses, this.pmRuntime, true));
		this.addImportStatements(imports, st);
		FileWriter.writeFile(
				interfaceName + ".java",
				RuntimeCommonDefinitions.getInstance()
						.getJavaDestinationFolder(this.outputDestinationFolder,
								packageName), st.render());
	}

	/**
	 * Get import statements from a given set of user library classes to be
	 * added in Java code.
	 * 
	 * @param userLibraryClasses
	 *            Set of user library classes.
	 * @param targetRuntime
	 *            A target runtime to get imports.
	 * @param isInterface
	 *            Specifies if imports will be inserted in an interface (true)
	 *            or a class (false).
	 */
	private Set<String> getImports(Set<String> userLibraryClasses,
			RuntimeDefinition targetRuntime, boolean isInterface)
			throws CompilationException {
		// Using a TreeSet here in order to keep imports sorted
		TreeSet<String> importStatements = new TreeSet<>();
		for (String userLibraryClass : userLibraryClasses) {
			if (isInterface) {
				importStatements.addAll(targetRuntime.getTranslator(
						userLibraryClass).getJavaInterfaceImports());
			} else {
				importStatements.addAll(targetRuntime.getTranslator(
						userLibraryClass).getJavaClassImports());
			}
		}
		return importStatements;
	}

	/**
	 * Add import statements in Java code.
	 * 
	 * @param importStatements
	 *            Collection with import statements.
	 * @param st
	 *            String template that will be used (must be able to used
	 *            addAggr method with "imports.{statement}" variable).
	 */
	private void addImportStatements(Collection<String> importStatements, ST st) {
		st.add("imports", null);
		for (String statement : importStatements) {
			st.addAggr("imports.{statement}", statement);
		}
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
	 * @param iteratorsAndBinds
	 *            Container with iterators and binds.
	 * @param methodCalls
	 *            Set of method calls that must be replaced.
	 * @param targetRuntime
	 *            Target runtime that will be used to create the class
	 *            implementation.
	 */
	private void createJavaWrapperImplementation(String packageName,
			String className, IteratorsAndBinds iteratorsAndBinds,
			List<MethodCall> methodCalls, RuntimeDefinition targetRuntime)
			throws CompilationException {
		String interfaceName = RuntimeCommonDefinitions.getInstance()
				.getJavaWrapperInterfaceName(className);
		String javaClassName = RuntimeCommonDefinitions.getInstance()
				.getJavaWrapperClassName(className,
						targetRuntime.getTargetRuntime());
		ST st = new ST(templateJavaClass);
		st.add("introductoryMsg", RuntimeCommonDefinitions.getInstance()
				.getHeaderComment());
		st.add("packageName", packageName);
		st.add("interfaceName", interfaceName);
		st.add("className", javaClassName);
		st.add("classDeclarations", null);
		Set<String> userLibraryClasses = new HashSet<String>();
		for (InputBind inputBind : iteratorsAndBinds.inputBinds) {
			UserLibraryTranslatorDefinition translator = targetRuntime
					.getTranslator(inputBind.variable.typeName);
			st.addAggr("classDeclarations.{line}",
					translator.translateInputBindObjDeclaration(inputBind));
			String methodSignature = RuntimeCommonDefinitions.getInstance()
					.createJavaMethodSignature(inputBind);
			String body = translator.translateInputBindObjCreation(
					javaClassName, inputBind);
			st.addAggr("methods.{signature, body}", methodSignature, body);
			userLibraryClasses.add(inputBind.variable.typeName);
		}
		for (String line : targetRuntime.getIsValidBody()) {
			st.add("isValidBody", line);
		}
		for (String line : targetRuntime.getInitializationString(className,
				iteratorsAndBinds, methodCalls)) {
			st.addAggr("classDeclarations.{line}", line);
		}
		for (Iterator iterator : iteratorsAndBinds.iterators) {
			String methodSignature = RuntimeCommonDefinitions.getInstance()
					.createJavaMethodSignature(iterator);
			String body = targetRuntime.getTranslator(
					iterator.variable.typeName).translateIteratorCall(
					javaClassName, iterator);
			st.addAggr("methods.{signature, body}", methodSignature, body);
			userLibraryClasses.add(iterator.variable.typeName);
		}
		for (OutputBind outputBind : iteratorsAndBinds.outputBinds) {
			String methodSignature = RuntimeCommonDefinitions.getInstance()
					.createJavaMethodSignature(outputBind);
			String body = targetRuntime.getTranslator(
					outputBind.variable.typeName).translateOutputBindCall(
					javaClassName, outputBind);
			st.addAggr("methods.{signature, body}", methodSignature, body);
			userLibraryClasses.add(outputBind.variable.typeName);
		}
		for (MethodCall methodCall : methodCalls) {
			String methodSignature = RuntimeCommonDefinitions.getInstance()
					.createJavaMethodSignature(methodCall);
			String body = targetRuntime.getTranslator(
					methodCall.variable.typeName).translateMethodCall(
					javaClassName, methodCall);
			st.addAggr("methods.{signature, body}", methodSignature, body);
			userLibraryClasses.add(methodCall.variable.typeName);
		}
		this.addImportStatements(
				this.getImports(userLibraryClasses, targetRuntime, false), st);
		this.addImportStatements(targetRuntime.getImports(), st);

		FileWriter.writeFile(
				javaClassName + ".java",
				RuntimeCommonDefinitions.getInstance()
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
	 * @param iteratorsAndBinds
	 *            Container with iterators and binds.
	 * @param methodCalls
	 *            Set of method calls that must be replaced.
	 * @param importTokens
	 *            Set of import tokens that must be removed.
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 */
	private void translateUserCode(String packageName, String className,
			ClassSymbol classSymbol, IteratorsAndBinds iteratorsAndBinds,
			Collection<MethodCall> methodCalls,
			Collection<TokenAddress> importTokens,
			TokenStreamRewriter tokenStreamRewriter)
			throws CompilationException {
		// Must find a better way to remove imports removing only those unused
		// imports, once some user library imports may still be in use.
		// this.removeUserLibraryImports(tokenStreamRewriter, importTokens);
		this.insertRenderScriptImports(classSymbol, tokenStreamRewriter);
		this.createRuntimeInstance(classSymbol, tokenStreamRewriter, className);
		this.translateInputBinds(tokenStreamRewriter, className,
				iteratorsAndBinds.inputBinds);
		this.translateIterators(tokenStreamRewriter,
				iteratorsAndBinds.iterators);
		this.translateOutputBinds(tokenStreamRewriter,
				iteratorsAndBinds.outputBinds);
		this.translateMethodCalls(tokenStreamRewriter, methodCalls);
		FileWriter.writeFile(
				className + ".java",
				RuntimeCommonDefinitions.getInstance()
						.getJavaDestinationFolder(this.outputDestinationFolder,
								packageName), tokenStreamRewriter.getText());
	}

	/**
	 * Insert RenderScript imports in user code.
	 * 
	 * @param classSymbol
	 *            Current class' symbol.
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @throws CompilationException
	 */
	private void insertRenderScriptImports(ClassSymbol classSymbol,
			TokenStreamRewriter tokenStreamRewriter)
			throws CompilationException {
		ST st = new ST("<imports:{var|import <var.statement>;\n}>\n)");
		this.addImportStatements(this.rsRuntime.getImports(), st);
		tokenStreamRewriter.insertBefore(classSymbol.tokenAddress.start,
				st.render());
	}

	/**
	 * Creates the code that is necessary for choosing, during runtime, which
	 * runtime will be executed.
	 * 
	 * @param classSymbol
	 *            Current class' symbol.
	 * @param tokenStreamRewriter
	 *            Token stream that will be used to rewrite user code.
	 * @param className
	 *            Name of the class of which current data (iterators and binds)
	 *            belong.
	 */
	private void createRuntimeInstance(ClassSymbol classSymbol,
			TokenStreamRewriter tokenStreamRewriter, String className) {
		String interfaceName = RuntimeCommonDefinitions.getInstance()
				.getJavaWrapperInterfaceName(className);
		String templateInitialization = "\n\n\tprivate <interfaceName> <objectName>;\n\n"
				+ "\tpublic <className>(RenderScript $mRS) {\n"
				+ "\t\tthis.<objectName> = new <openCLClassName>();\n"
				+ "\t\tif (this.<objectName>.isValid())\n"
				+ "\t\t\tthis.<objectName> = new <renderScriptClassName>($mRS);\n"
				+ "\t}\n";
		ST st = new ST(templateInitialization);
		st.add("interfaceName", interfaceName);
		st.add("objectName", RuntimeCommonDefinitions.getInstance()
				.getParallelMEObjectName());
		st.add("className", className);
		st.add("openCLClassName", RuntimeCommonDefinitions.getInstance()
				.getJavaWrapperClassName(className, TargetRuntime.ParallelME));
		st.add("renderScriptClassName", RuntimeCommonDefinitions.getInstance()
				.getJavaWrapperClassName(className, TargetRuntime.RenderScript));
		tokenStreamRewriter.insertAfter(classSymbol.bodyAddress.start,
				st.render());
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
		String objectName = RuntimeCommonDefinitions.getInstance()
				.getParallelMEObjectName();
		for (InputBind inputBind : inputBinds) {
			String translatedStatement = String.format("%s.%s(%s);",
					objectName, RuntimeCommonDefinitions.getInstance()
							.getInputBindName(inputBind),
					RuntimeCommonDefinitions.getInstance()
							.toCommaSeparatedString(inputBind.parameters));
			tokenStreamRewriter.replace(
					inputBind.creationStatementAddress.start,
					inputBind.creationStatementAddress.stop,
					translatedStatement);
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
		String objectName = RuntimeCommonDefinitions.getInstance()
				.getParallelMEObjectName();
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
				st.add("iteratorName", RuntimeCommonDefinitions.getInstance()
						.getIteratorName(iterator));
				for (Variable variable : iterator.getExternalVariables()) {
					if (!variable.isFinal()) {
						String arrName = RuntimeCommonDefinitions.getInstance()
								.getPrefix() + variable.name;
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
				translatedStatement = String.format(
						"%s.%s(%s);",
						objectName,
						RuntimeCommonDefinitions.getInstance().getIteratorName(
								iterator),
						RuntimeCommonDefinitions.getInstance()
								.toCommaSeparatedString(
										iterator.getExternalVariables()));
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
		String objectName = RuntimeCommonDefinitions.getInstance()
				.getParallelMEObjectName();
		for (OutputBind outputBind : outputBinds) {
			String translatedStatement;
			if (outputBind.outputBindType == OutputBindType.DeclarativeAssignment) {
				translatedStatement = String.format("%s %s;\n%s.%s(%s);",
						outputBind.destinationObject.typeName,
						outputBind.destinationObject.name, objectName,
						RuntimeCommonDefinitions.getInstance()
								.getOutputBindName(outputBind),
						outputBind.destinationObject.name);
			} else {
				translatedStatement = String.format("%s.%s(%s);", objectName,
						RuntimeCommonDefinitions.getInstance()
								.getOutputBindName(outputBind),
						outputBind.destinationObject.name);
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
		String objectName = RuntimeCommonDefinitions.getInstance()
				.getParallelMEObjectName();
		for (MethodCall methodCall : methodCalls) {
			String translatedStatement = String.format(
					"%s.%s()",
					objectName,
					RuntimeCommonDefinitions.getInstance().getMethodCallName(
							methodCall));
			tokenStreamRewriter.replace(methodCall.expressionAddress.start,
					methodCall.expressionAddress.stop, translatedStatement);
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
	 * Split iterators and binds into separate lists, putting them on a specific
	 * container.
	 * 
	 * @param iteratorsAndBinds
	 *            List of all iterators and binds found.
	 * @param symbolTable
	 *            Symbol table.
	 * 
	 * @return Container with input binds, iterators and output binds.
	 */
	private IteratorsAndBinds getIteratorsAndBinds(
			Collection<UserLibraryData> iteratorsAndBinds, Symbol symbolTable) {
		ArrayList<Iterator> iterators = new ArrayList<>();
		ArrayList<OutputBind> outputBinds = new ArrayList<>();
		for (UserLibraryData userLibraryData : iteratorsAndBinds) {
			if (userLibraryData instanceof Iterator) {
				Iterator iterator = (Iterator) userLibraryData;
				this.setIteratorType(iterator);
				iterators.add(iterator);
			} else if (userLibraryData instanceof OutputBind) {
				outputBinds.add((OutputBind) userLibraryData);
			}
		}
		ArrayList<InputBind> inputBinds = new ArrayList<>();
		int inputBindCount = 0;
		for (Pair<UserLibraryVariableSymbol, CreatorSymbol> pair : this
				.getVariablesWithCreators(symbolTable)) {
			VariableSymbol variableSymbol = (VariableSymbol) pair.left;
			Variable variable = new Variable(variableSymbol.name,
					variableSymbol.typeName, variableSymbol.typeParameterName,
					variableSymbol.modifier, variableSymbol.identifier);
			Parameter[] arguments = this
					.argumentsToVariableParameter(pair.right.arguments);
			inputBinds.add(new InputBind(variable, ++inputBindCount, arguments,
					pair.left.statementAddress, pair.right.statementAddress));
		}
		return new IteratorsAndBinds(inputBinds, iterators, outputBinds);
	}

	/**
	 * Check an iterator and find out if it is a parallel or sequential
	 * iterator. Parallel iterators must have ALL external variables final,
	 * whereas iterators that contains non-const variables will be compiled to
	 * sequential versions in the target runtime.
	 */
	private void setIteratorType(Iterator iterator) {
		Variable[] variables = iterator.getExternalVariables();
		Iterator.IteratorType iteratorType = IteratorType.Parallel;
		for (int i = 0; i < variables.length
				&& iteratorType == IteratorType.Parallel; i++) {
			if (!variables[i].isFinal()) {
				SimpleLogger
						.warn("Iterator with non-final external variable in line "
								+ iterator.statementAddress.start.getLine()
								+ " will be translated to a sequential iterator in the target runtime.");
				iteratorType = IteratorType.Sequential;
			}
		}
		iterator.setType(iteratorType);
	}
}
