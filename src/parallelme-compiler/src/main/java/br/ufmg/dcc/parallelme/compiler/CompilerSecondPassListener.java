/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import java.util.*;

import org.antlr.v4.runtime.TokenStream;

import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser;
import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser.ExpressionContext;
import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser.VariableDeclaratorContext;
import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser.VariableInitializerContext;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Iterator;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.MethodCall;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.OutputBind;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.UserFunction;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.UserLibraryData;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Variable;
import br.ufmg.dcc.parallelme.compiler.symboltable.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.*;

/**
 * Translates the code written in the iterator.
 * 
 * @author Wilson de Carvalho
 */
public class CompilerSecondPassListener extends ScopeDrivenListener {
	private enum StatementType {
		Iterator, OutputBind, None;
	}

	// Enumeration that is used to indicate what type of statement is currently
	// being evaluated.
	private StatementType statementType = StatementType.None;
	// Stores the user library variables under the scope.
	private Map<String, Symbol> userLibraryVariablesUnderScope;
	// Stores all those libraries that are used inside an interator, but are
	// declared outside its method scope.
	private Map<String, VariableSymbol> iteratorExternalVariables;
	private Iterator currentIteratorData;
	private final ArrayList<UserLibraryData> iteratorsAndBinds;
	// Token stream used to extract original code data.
	private TokenStream tokenStream;
	// Used to calculate the unique number of a iterator or bind during its
	// creation.
	private int lastFunctionCount;
	// Stores package name.
	private String packageName;
	// List of those tokens that must be removed from the output code.
	private final ArrayList<TokenAddress> importTokens = new ArrayList<>();
	// List of those method calls on user library objects (methods that are not
	// output bind or iterators).
	private final ArrayList<MethodCall> methodCalls = new ArrayList<>();

	/**
	 * Constructor.
	 * 
	 * @param tokenStream
	 *            Token stream for the tree being visited. Used to extract
	 *            original code data.
	 * @param lastFunctionCount
	 *            Used to calculate the unique number of iterators and binds
	 *            during creation.
	 * 
	 */
	public CompilerSecondPassListener(TokenStream tokenStream,
			int lastFunctionCount) {
		super(new RootSymbol());
		this.iteratorsAndBinds = new ArrayList<>();
		this.tokenStream = tokenStream;
		this.lastFunctionCount = lastFunctionCount;
	}

	/**
	 * List user library iterators that must be translated to the target
	 * runtime.
	 * 
	 * @return A collection of iterator data.
	 */
	public ArrayList<UserLibraryData> getIteratorsAndBinds() {
		return this.iteratorsAndBinds;
	}

	/**
	 * List the file's package name.
	 * 
	 * @return Package name.
	 */
	public String getPackageName() {
		return this.packageName;
	}

	/**
	 * List user library import statements.
	 * 
	 * @return A collection of token addresses.
	 */
	public Collection<TokenAddress> getImportTokens() {
		return this.importTokens;
	}

	/**
	 * List of method calls expressions.
	 * 
	 * @return A collection of non-iterator and non-output bind method calls.
	 */
	public Collection<MethodCall> getMethodCalls() {
		return this.methodCalls;
	}

	/**
	 * Insert those import declarations that contains ParallelME packages in the
	 * list of imports that must be removed from the output code.
	 * 
	 * @param ctx
	 *            Import context.
	 */
	@Override
	public void enterImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
		String importDeclaration = ctx.qualifiedName().getText();
		if (importDeclaration.contains(PackageDefinition.getBasePackage())) {
			this.importTokens.add(new TokenAddress(ctx.start, ctx.stop));
		}
	}

	/**
	 * Stores the file's package name in a public variable.
	 */
	@Override
	public void enterPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
		this.packageName = ctx.qualifiedName().getText();
	}

	@Override
	public void exitBlock(JavaParser.BlockContext ctx) {
		this.finalizeIteratorDetection();
	}

	/**
	 * Finalizes the iterator block detection.
	 */
	private void finalizeIteratorDetection() {
		// If a user library iterator data was created, then we must fill the
		// remaining data.
		if (this.statementType == StatementType.Iterator
				&& this.currentIteratorData != null) {
			this.getIteratorData();
			this.currentIteratorData = null;
			this.iteratorExternalVariables = null;
		}
		this.statementType = StatementType.None;
	}

	/**
	 * Extracts the necessary iterator data to create an object with user
	 * function data.
	 * 
	 * TODO This code must be refactored. This data should be get in the
	 * isIterator method. I got this data here because I'm using an instance of
	 * ScopeDrivenListener that builds the symbol table along the tree walk. It
	 * must be modified to provide scopes based on the symbol table created on
	 * the first pass, otherwise this translator will never work correctly in
	 * case a user class variable is declared after a given method and then used
	 * inside this method.
	 */
	private void getIteratorData() {
		if (this.currentScope.enclosingScope instanceof CreatorSymbol) {
			if (!(this.currentScope instanceof MethodSymbol)) {
				// TODO Must point to an error here. This method must be called
				// uniquely if a user library iterator was detected.
				return;
			}
			// Looks for a method body in the current method scope
			MethodSymbol method = (MethodSymbol) this.currentScope;
			MethodBodySymbol methodBody = null;
			for (Symbol symbol : method.innerSymbols.values()) {
				if (symbol instanceof MethodBodySymbol) {
					methodBody = (MethodBodySymbol) symbol;
				}
			}
			if (methodBody == null) {
				// TODO Must point to an error here. This method must be called
				// uniquely if a user library iterator with body was detected.
				return;
			}
			if (!method.arguments.isEmpty()) {
				Symbol argument = method.arguments.iterator().next();
				if (argument instanceof VariableSymbol) {
					VariableSymbol argumentVariable = (VariableSymbol) argument;
					String originalMethodContent = tokenStream.getText(
							methodBody.tokenAddress.start,
							methodBody.tokenAddress.stop);
					UserFunction userFunctionData = new UserFunction(
							originalMethodContent, new Variable(
									argumentVariable.name,
									argumentVariable.typeName,
									argumentVariable.typeParameterName,
									argumentVariable.modifier));
					// Add all those external variables found on the iterator to
					// be used in the second pass.
					for (VariableSymbol variable : this.iteratorExternalVariables
							.values()) {
						this.currentIteratorData
								.addExternalVariable(new Variable(
										variable.name, variable.typeName,
										variable.typeParameterName,
										variable.modifier));
					}
					this.currentIteratorData
							.setUserFunctionData(userFunctionData);
					this.iteratorsAndBinds.add(this.currentIteratorData);
				}
			}
		}
	}

	@Override
	public void enterExpression(JavaParser.ExpressionContext ctx) {
		this.checkExpression(ctx);
	}

	private void checkExpression(JavaParser.ExpressionContext ctx) {
		String expression = ctx.getText();
		// To avoid unnecessary user variable check, only performs verification
		// as long as there is a different scope.
		if (this.currentScope != this.previousScope) {
			this.userLibraryVariablesUnderScope = this.currentScope
					.getSymbolsUnderScope(UserLibraryVariableSymbol.class);

		}
		// Checks if this expression contains an user library variable
		if (this.userLibraryVariablesUnderScope.containsKey(expression)) {
			UserLibraryVariableSymbol variable = (UserLibraryVariableSymbol) this.userLibraryVariablesUnderScope
					.get(expression);
			UserLibraryClass userLibraryClass = UserLibraryClassFactory
					.create(variable.typeName);
			// Check if the declared object is a collection
			if (userLibraryClass instanceof UserLibraryCollectionClassImpl) {
				if (this.isIterator(variable, this.currentStatement, ctx)) {
					this.statementType = StatementType.Iterator;
					this.iteratorExternalVariables = new LinkedHashMap<>();
					this.getIteratorData(variable);
				} else if (this.isOutputBind(variable,
						(UserLibraryCollectionClassImpl) userLibraryClass, ctx)) {
					this.statementType = StatementType.OutputBind;
					this.getOutputBindData(variable, ctx);
				} else if (this.isValidMethod(
						(UserLibraryCollectionClassImpl) userLibraryClass, ctx)) {
					this.getMethodCallData(variable, ctx);
				}
			}
		}
		// Check if this expression is equivalent to a symbol that is not under
		// this scope, but is under the enclosing scope. In this case, it is
		// possible to find all those variables that are used in a user function
		// implementation, but was in fact declared outside its scope.
		if (this.statementType == StatementType.Iterator) {
			Symbol variable = this.currentScope.getInnerSymbol(expression,
					VariableSymbol.class);
			if (variable == null) {
				VariableSymbol variableEncScope = (VariableSymbol) this.currentScope.enclosingScope
						.getSymbolUnderScope(expression, VariableSymbol.class);
				if (variableEncScope != null) {
					this.iteratorExternalVariables.put(variableEncScope.name,
							variableEncScope);
				}
			}
		}
	}

	/**
	 * Checks if the statement provided that contains a user library object
	 * corresponds to an iterator that must be translated to the target runtime.
	 * 
	 * @param variable
	 *            User library variable symbol that corresponds to the statement
	 *            and expression provided.
	 * @param stx
	 *            Statement containing a user library object.
	 * @param etx
	 *            Expression containing a user library object.
	 */
	private boolean isIterator(UserLibraryVariableSymbol variable,
			JavaParser.StatementContext stx, JavaParser.ExpressionContext etx) {
		boolean ret = false;
		// Work on the following cases:
		// - variableName.par().foreach(...)
		if (etx.parent.getText().equals(variable.name + ".par")
				&& stx.statementExpression() != null
				&& stx.statementExpression().expression() != null
				&& stx.statementExpression().expression().expressionList() != null
				&& !stx.statementExpression().expression().expressionList()
						.isEmpty()) {
			// This statement must have its expressions evaluated
			this.statementType = StatementType.Iterator;
			ret = true;
		}
		return ret;
	}

	/**
	 * Gets the iterator data from a variable symbol and stores on the
	 * currentIteratorData object.
	 * 
	 * @param variable
	 *            User library variable symbol that corresponds to the iterator
	 *            variable.
	 */
	private void getIteratorData(UserLibraryVariableSymbol variable) {
		Variable variableParameter = new Variable(variable.name,
				variable.typeName, variable.typeParameterName,
				variable.modifier);
		this.currentIteratorData = new Iterator(variableParameter,
				this.iteratorsAndBinds.size() + this.lastFunctionCount,
				new TokenAddress(this.currentStatement.start,
						this.currentStatement.stop));
	}

	/**
	 * Checks if the expression provided that contains a user library object
	 * corresponds to an output bind that must be translated to the target
	 * runtime.
	 */
	private boolean isOutputBind(UserLibraryVariableSymbol variable,
			UserLibraryCollectionClass userLibraryClass,
			JavaParser.ExpressionContext ctx) {
		boolean ret = false;
		if (ctx.parent.getText().equals(
				variable.name + "."
						+ userLibraryClass.getDataOutputMethodName())) {
			ret = true;
		}
		return ret;
	}

	/**
	 * Checks if the expression provided that contains a user library object
	 * corresponds to a valid method call.
	 */
	private boolean isValidMethod(UserLibraryCollectionClass userLibraryClass,
			JavaParser.ExpressionContext ctx) {
		boolean ret = false;
		if (ctx.parent.parent instanceof ExpressionContext) {
			String expression = ctx.parent.getText();
			String methodName = expression.substring(
					expression.indexOf(".") + 1, expression.length());
			if (userLibraryClass.isValidMethod(methodName)) {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * Gets the output bind data from a statement and stores on the outputBinds
	 * array.
	 * 
	 * The only format accepted for the compiler prototype is:
	 * destinationVariable = variable.outputMethod();
	 * 
	 * @param variable
	 *            User library variable symbol that corresponds to the statement
	 *            and expression provided.
	 */
	private void getOutputBindData(
			UserLibraryVariableSymbol userLibraryVariableSymbol,
			JavaParser.ExpressionContext ctx) {
		String destinationVariableName = null;
		// Will indicate if the output bind statement is also an object
		// declaration
		boolean isObjectDeclaration = false;
		// Type varName = var.method();
		if (ctx.parent.parent.parent instanceof VariableInitializerContext) {
			VariableDeclaratorContext foo = (VariableDeclaratorContext) ctx.parent.parent.parent.parent;
			destinationVariableName = foo.variableDeclaratorId().getText();
			isObjectDeclaration = true;
		} else if (ctx.parent.parent.parent instanceof ExpressionContext) {
			ExpressionContext foo = (ExpressionContext) ctx.parent.parent.parent;
			// varName = var.method();
			if (!foo.expression().isEmpty()
					&& foo.expression(0).primary() != null) {
				destinationVariableName = foo.expression(0).primary().getText();
			} else if (foo.type() != null
					&& foo.parent.parent instanceof VariableDeclaratorContext) {
				// Type varName = (castType) var.method();
				destinationVariableName = ((VariableDeclaratorContext) foo.parent.parent)
						.variableDeclaratorId().getText();
				isObjectDeclaration = true;
			} else {
				// varName = (castType) var.method();
				destinationVariableName = ((ExpressionContext) foo.parent)
						.expression(0).primary().getText();
			}
		} else {
			String errorMsg = "Invalid output bind statement at line "
					+ ctx.start.getLine()
					+ ". Output bind statements must be of form "
					+ "'variable = object.outputBindMethod();'";
			SimpleLogger.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
		if (destinationVariableName != null) {
			Symbol destinationSymbol = this.currentScope
					.getInnerSymbol(destinationVariableName);
			if (destinationSymbol instanceof VariableSymbol) {
				VariableSymbol destinationVariableSymbol = (VariableSymbol) destinationSymbol;
				Variable destinationVariable = new Variable(
						destinationVariableSymbol.name,
						destinationVariableSymbol.typeName,
						destinationVariableSymbol.typeParameterName,
						destinationVariableSymbol.modifier);
				Variable userLibraryVariable = new Variable(
						userLibraryVariableSymbol.name,
						userLibraryVariableSymbol.typeName,
						userLibraryVariableSymbol.typeParameterName,
						userLibraryVariableSymbol.modifier);
				TokenAddress tokenAddress;
				if (this.currentStatement != null)
					tokenAddress = new TokenAddress(
							this.currentStatement.start,
							this.currentStatement.stop);
				else
					tokenAddress = new TokenAddress(
							this.currentVariableStatement.start,
							this.currentVariableStatement.stop);
				this.iteratorsAndBinds.add(new OutputBind(userLibraryVariable,
						destinationVariable, this.iteratorsAndBinds.size()
								+ lastFunctionCount, tokenAddress,
						isObjectDeclaration));
			}
		}
	}

	/**
	 * Gets the method call data and stores in methodCalls array.
	 * 
	 * @param variable
	 *            User library variable symbol that corresponds to the
	 *            expression provided.
	 */
	private void getMethodCallData(UserLibraryVariableSymbol variable,
			JavaParser.ExpressionContext ctx) {
		ExpressionContext expressionCtx = (ExpressionContext) ctx.parent.parent;
		String expression = ctx.parent.getText();
		String methodName = expression.substring(expression.indexOf(".") + 1,
				expression.length());
		this.methodCalls.add(new MethodCall(methodName, new Variable(
				variable.name, variable.typeName, variable.typeParameterName,
				variable.modifier), new TokenAddress(expressionCtx.start,
				expressionCtx.stop)));
	}
}
