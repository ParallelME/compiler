/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.util.*;

import org.antlr.v4.runtime.TokenStream;
import org.parallelme.compiler.antlr.JavaParser;
import org.parallelme.compiler.antlr.JavaParser.ExpressionContext;
import org.parallelme.compiler.antlr.JavaParser.LocalVariableDeclarationStatementContext;
import org.parallelme.compiler.antlr.JavaParser.StatementExpressionContext;
import org.parallelme.compiler.antlr.JavaParser.VariableDeclaratorContext;
import org.parallelme.compiler.antlr.JavaParser.VariableInitializerContext;
import org.parallelme.compiler.intermediate.Operation;
import org.parallelme.compiler.intermediate.MethodCall;
import org.parallelme.compiler.intermediate.Operation.OperationType;
import org.parallelme.compiler.intermediate.OutputBind;
import org.parallelme.compiler.intermediate.OutputBind.OutputBindType;
import org.parallelme.compiler.intermediate.UserFunction;
import org.parallelme.compiler.intermediate.UserLibraryData;
import org.parallelme.compiler.intermediate.Variable;
import org.parallelme.compiler.symboltable.*;
import org.parallelme.compiler.userlibrary.*;

/**
 * The second pass is responsible for creating the intermediate representation,
 * which will decouple the symbol table from code translation.
 * 
 * @author Wilson de Carvalho
 */
public class CompilerSecondPassListener extends ScopeDrivenListener {
	private enum StatementType {
		Operation, OutputBind, None;
	}

	// Enumeration that is used to indicate what type of statement is currently
	// being evaluated.
	private StatementType statementType = StatementType.None;
	// Stores the user library variables under the scope.
	private Map<String, Symbol> userLibraryVariablesUnderScope;
	// Stores all those libraries that are used inside an operation, but are
	// declared outside its method scope.
	private Map<String, VariableSymbol> operationExternalVariables;
	private Operation currentOperationData;
	private final ArrayList<UserLibraryData> operationsAndBinds;
	// Token stream used to extract original code data.
	private TokenStream tokenStream;
	// Used to calculate the unique number for operations.
	private int operationCount;
	// Used to calculate the unique number for output binds.
	private int outputBindCount;
	// Used to calculate the unique number for method calls.
	private int methodCallCount;
	// Stores package name.
	private String packageName;
	// Stores current operation name
	private String operationName;
	// List of those tokens that must be removed from the output code.
	private final ArrayList<TokenAddress> importTokens = new ArrayList<>();
	// List of those method calls on user library objects (methods that are not
	// output bind or operations).
	private final ArrayList<MethodCall> methodCalls = new ArrayList<>();

	/**
	 * Constructor.
	 * 
	 * @param tokenStream
	 *            Token stream for the tree being visited. Used to extract
	 *            original code data.
	 */
	public CompilerSecondPassListener(TokenStream tokenStream) {
		super(new RootSymbol());
		this.operationsAndBinds = new ArrayList<>();
		this.tokenStream = tokenStream;
		this.operationCount = this.outputBindCount = this.methodCallCount = 0;
	}

	/**
	 * List user library operations that must be translated to the target
	 * runtime.
	 * 
	 * @return A collection of operations and binds.
	 */
	public ArrayList<UserLibraryData> getOperationsAndBinds() {
		return this.operationsAndBinds;
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
	public List<TokenAddress> getImportTokens() {
		return this.importTokens;
	}

	/**
	 * List of method calls expressions.
	 * 
	 * @return A collection of non-operations and non-output bind method calls.
	 */
	public List<MethodCall> getMethodCalls() {
		return this.methodCalls;
	}

	/**
	 * Number of operation objects created during the parse walk.
	 */
	public int getOperationCount() {
		return this.operationCount;
	}

	/**
	 * Number of OutputBind objects created during the parse walk.
	 */
	public int getOutputBindCount() {
		return this.outputBindCount;
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
		this.finalizeOperationDetection();
	}

	/**
	 * Finalizes the operation block detection.
	 */
	private void finalizeOperationDetection() {
		// If a user library operation data was created, then we must fill the
		// remaining data.
		if (this.statementType == StatementType.Operation
				&& this.currentOperationData != null) {
			this.getOperationData();
			this.currentOperationData = null;
			this.operationExternalVariables = null;
		}
		this.statementType = StatementType.None;
	}

	/**
	 * Extracts the necessary operation data to create an object with user
	 * function data.
	 * 
	 * TODO This code must be refactored. This data should be get in the
	 * isOperation method. I got this data here because I'm using an instance of
	 * ScopeDrivenListener that builds the symbol table along the tree walk. It
	 * must be modified to provide scopes based on the symbol table created on
	 * the first pass, otherwise this translator will never work correctly in
	 * case a user class variable is declared after a given method and then used
	 * inside this method.
	 */
	private void getOperationData() {
		if (this.currentScope.enclosingScope instanceof CreatorSymbol) {
			if (!(this.currentScope instanceof MethodSymbol)) {
				// TODO Must point to an error here. This method must be called
				// uniquely if a user library operation was detected.
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
				throw new RuntimeException("Operation with no method body.");
			}
			if (!method.arguments.isEmpty()) {
				ArrayList<Variable> variables = new ArrayList<>();
				for (Symbol argument : method.arguments) {
					if (argument instanceof VariableSymbol) {
						VariableSymbol argumentVariable = (VariableSymbol) argument;
						variables.add(new Variable(argumentVariable.name,
								argumentVariable.typeName,
								argumentVariable.typeParameterName,
								argumentVariable.modifier,
								argumentVariable.identifier));
					}
				}
				String originalMethodContent = tokenStream.getText(
						methodBody.tokenAddress.start,
						methodBody.tokenAddress.stop);
				UserFunction userFunctionData = new UserFunction(
						originalMethodContent, variables);
				// Add all those external variables found on the operation to
				// be used in the second pass.
				for (VariableSymbol variable : this.operationExternalVariables
						.values()) {
					this.currentOperationData.addExternalVariable(new Variable(
							variable.name, variable.typeName,
							variable.typeParameterName, variable.modifier,
							variable.identifier));
				}
				this.currentOperationData.setUserFunctionData(userFunctionData);
				this.operationsAndBinds.add(this.currentOperationData);
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
					.getClass(variable.typeName);
			// Check if the declared object is a collection
			if (userLibraryClass instanceof UserLibraryCollectionClass) {
				if (this.isOperation(variable, ctx)) {
					this.statementType = StatementType.Operation;
					this.operationExternalVariables = new LinkedHashMap<>();
					this.createOperation(variable, ctx);
				} else if (this.isOutputBind(variable,
						(UserLibraryCollectionClass) userLibraryClass, ctx)) {
					this.statementType = StatementType.OutputBind;
					this.getOutputBindData(variable, ctx);
				} else if (this.isValidMethod(
						(UserLibraryCollectionClass) userLibraryClass, ctx)) {
					this.getMethodCallData(variable, ctx);
				}
			}
		}
		// Check if this expression is equivalent to a symbol that is not under
		// this scope, but is under the enclosing scope. In this case, it is
		// possible to find all those variables that are used in a user function
		// implementation, but was in fact declared outside its scope.
		if (this.statementType == StatementType.Operation) {
			Symbol variable = this.currentScope.getInnerSymbol(expression,
					VariableSymbol.class);
			if (variable == null) {
				VariableSymbol variableEncScope = (VariableSymbol) this.currentScope.enclosingScope
						.getSymbolUnderScope(expression, VariableSymbol.class);
				if (variableEncScope != null) {
					this.operationExternalVariables.put(variableEncScope.name,
							variableEncScope);
				}
			}
		}
	}

	/**
	 * Checks if the statement provided that contains a user library object
	 * corresponds to an operation that must be translated to the target
	 * runtime.
	 * 
	 * @param variable
	 *            User library variable symbol that corresponds to the statement
	 *            and expression provided.
	 * @param etx
	 *            Expression containing a user library object.
	 */
	private boolean isOperation(UserLibraryVariableSymbol variable,
			JavaParser.ExpressionContext etx) {
		boolean ret = false;
		// Work on the following cases:
		// - variableName.par().operationName(...)
		JavaParser.StatementContext stx = this.currentStatement;
		LocalVariableDeclarationStatementContext lcx = this.currentVariableStatement;
		if (etx.parent.getText().equals(variable.name + ".par")) {
			if ((stx != null
					&& stx.statementExpression() != null
					&& stx.statementExpression().expression() != null
					&& stx.statementExpression().expression().expressionList() != null
					&& !stx.statementExpression().expression().expressionList()
							.isEmpty() && etx.parent.parent.parent instanceof ExpressionContext)
					|| (lcx != null && lcx.localVariableDeclaration() != null)) {
				// This statement must have its expressions evaluated
				this.statementType = StatementType.Operation;
				this.operationName = ((ExpressionContext) etx.parent.parent.parent)
						.Identifier().getText();
				ret = true;
			} else {
				throw new RuntimeException("Unsupported statement.");
			}
		}
		return ret;
	}

	/**
	 * Gets the operation data from a variable symbol and stores on the
	 * currentOperationData object.
	 * 
	 * @param variableSymbol
	 *            User library variable symbol that corresponds to the operation
	 *            variable.
	 */
	private void createOperation(UserLibraryVariableSymbol variableSymbol,
			JavaParser.ExpressionContext etx) {
		Variable variable = new Variable(variableSymbol.name,
				variableSymbol.typeName, variableSymbol.typeParameterName,
				variableSymbol.modifier, variableSymbol.identifier);
		OperationType operationType;
		Variable destinationVariable = null;
		if (UserLibraryCollectionClass.getOperationMethods().contains(
				this.operationName)) {
			if (this.operationName.equals(UserLibraryCollectionClass
					.getForeachMethodName())) {
				operationType = OperationType.Foreach;
				this.currentOperationData = new Operation(variable,
						++operationCount, new TokenAddress(
								this.currentStatement.start,
								this.currentStatement.stop), operationType,
						destinationVariable);
			} else {
				operationType = OperationType.Reduce;
				LocalVariableDeclarationStatementContext lcx = this.currentVariableStatement;
				String variableName = lcx.localVariableDeclaration()
						.variableDeclarators().variableDeclarator(0)
						.variableDeclaratorId().getText();
				destinationVariable = this.getVariable(variableName);
				this.currentOperationData = new Operation(variable,
						++operationCount, new TokenAddress(
								this.currentVariableStatement.start,
								this.currentVariableStatement.stop),
						operationType, destinationVariable);
			}
		} else {
			throw new RuntimeException("Unsupported operation: "
					+ this.operationName);
		}
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
						+ userLibraryClass.getOutputBindMethodName())) {
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
	 * The only formats accepted for the compiler prototype are: 1. Type
	 * destinationVariable = variable.outputBindMethod(); 2. destinationVariable
	 * = variable.outputBindMethod(); 3.
	 * variable.outputBindMethod(destinationVariable);
	 * 
	 * @param variable
	 *            User library variable symbol that corresponds to the statement
	 *            and expression provided.
	 */
	private void getOutputBindData(
			UserLibraryVariableSymbol userLibraryVariableSymbol,
			JavaParser.ExpressionContext ctx) {
		String destinationVariableName = null;
		// Will indicate if the output bind statement is just an assignment or
		// is also an object declaration.
		OutputBindType outputBindType = OutputBindType.None;
		ExpressionContext exp = (ExpressionContext) ctx.parent.parent;
		// Type varName = var.method();
		if (ctx.parent.parent.parent instanceof VariableInitializerContext) {
			VariableDeclaratorContext foo = (VariableDeclaratorContext) ctx.parent.parent.parent.parent;
			destinationVariableName = foo.variableDeclaratorId().getText();
			outputBindType = OutputBindType.DeclarativeAssignment;
		} else if (ctx.parent.parent.parent instanceof ExpressionContext) {
			ExpressionContext foo = (ExpressionContext) ctx.parent.parent.parent;
			// varName = var.method();
			if (!foo.expression().isEmpty()
					&& foo.expression(0).primary() != null) {
				destinationVariableName = foo.expression(0).primary().getText();
				outputBindType = OutputBindType.Assignment;
			} else if (foo.type() != null
					&& foo.parent.parent instanceof VariableDeclaratorContext) {
				// Type varName = (castType) var.method();
				destinationVariableName = ((VariableDeclaratorContext) foo.parent.parent)
						.variableDeclaratorId().getText();
				outputBindType = OutputBindType.DeclarativeAssignment;
			} else {
				// varName = (castType) var.method();
				destinationVariableName = ((ExpressionContext) foo.parent)
						.expression(0).primary().getText();
				outputBindType = OutputBindType.Assignment;
			}
		} else if (ctx.parent instanceof ExpressionContext
				&& ctx.parent.parent instanceof ExpressionContext
				&& ctx.parent.parent.parent instanceof StatementExpressionContext
				&& exp.expressionList() != null) {
			destinationVariableName = exp.expressionList().getText();
		} else {
			String errorMsg = "Invalid output bind statement at line "
					+ ctx.start.getLine()
					+ ". Output bind statements must be of form "
					+ "'variable = object.outputBindMethod();' or "
					+ "'object.outputBindMethod(variable);'";
			SimpleLogger.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}
		if (destinationVariableName != null) {
			Variable destinationVariable = this
					.getVariable(destinationVariableName);
			if (destinationVariable != null) {
				Variable userLibraryVariable = this
						.getVariable(userLibraryVariableSymbol);
				TokenAddress tokenAddress;
				if (this.currentStatement != null)
					tokenAddress = new TokenAddress(
							this.currentStatement.start,
							this.currentStatement.stop);
				else
					tokenAddress = new TokenAddress(
							this.currentVariableStatement.start,
							this.currentVariableStatement.stop);
				this.operationsAndBinds.add(new OutputBind(userLibraryVariable,
						destinationVariable, ++this.outputBindCount,
						tokenAddress, outputBindType));
			} else {
				// TODO Throw exception or show error here
			}
		} else {
			String errorMsg = "Invalid output bind statement at line "
					+ ctx.start.getLine()
					+ ". Output bind statements must be of form "
					+ "'object.outputBindMethod(variableParam);'";
			SimpleLogger.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}

	}

	public Variable getVariable(String variableName) {
		Symbol symbol = this.currentScope.getSymbolUnderScope(variableName);
		if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			return new Variable(variableSymbol.name, variableSymbol.typeName,
					variableSymbol.typeParameterName, variableSymbol.modifier,
					variableSymbol.identifier);
		} else {
			return null;
		}
	}

	public Variable getVariable(
			UserLibraryVariableSymbol userLibraryVariableSymbol) {
		return new Variable(userLibraryVariableSymbol.name,
				userLibraryVariableSymbol.typeName,
				userLibraryVariableSymbol.typeParameterName,
				userLibraryVariableSymbol.modifier,
				userLibraryVariableSymbol.identifier);
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
				variable.modifier, variable.identifier), new TokenAddress(
				expressionCtx.start, expressionCtx.stop),
				++this.methodCallCount));
	}
}
