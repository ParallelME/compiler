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
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.TokenStream;

import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser;
import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser.ExpressionContext;
import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser.StatementContext;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.data.Iterator;
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
public class TranslatorSecondPassListener extends ScopeDrivenListener {
	private enum StatementType {
		Iterator, OutputBind, None;
	}

	// Enumeration that is used to indicate what type of statement is currently
	// being evaluated.
	private StatementType statementType = StatementType.None;
	// Stores the user library variables under the scope.
	private Map<String, Symbol> userLibraryVariablesUnderScope;
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
	private final ArrayList<TokenAddress> importTokens = new ArrayList<TokenAddress>();

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
	public TranslatorSecondPassListener(TokenStream tokenStream,
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
	public void exitStatement(JavaParser.StatementContext ctx) {
		super.exitStatement(ctx);
		this.finalizeIteratorDetection();
	}

	/**
	 * Finalizes the iterator statement detection.
	 */
	private void finalizeIteratorDetection() {
		// If a user library iterator data was created, then we must fill the
		// remaining data.
		if (this.statementType == StatementType.Iterator
				&& this.currentIteratorData != null) {
			this.getIteratorData();
			this.currentIteratorData = null;
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
									argumentVariable.typeParameterName));
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
		// To avoid unnecessary user variable check, only performs verification
		// as long as there is a different scope.
		if (this.currentScope != this.previousScope) {
			this.userLibraryVariablesUnderScope = this.currentScope
					.getSymbolsUnderScope(UserLibraryVariableSymbol.class);
		}
		// Checks if this expression contains an user library variable
		if (this.userLibraryVariablesUnderScope.containsKey(ctx.getText())) {
			UserLibraryVariableSymbol variable = (UserLibraryVariableSymbol) this.userLibraryVariablesUnderScope
					.get(ctx.getText());
			UserLibraryClass userLibraryClass = UserLibraryClassFactory
					.create(variable.typeName);
			// Check if the declared object is a collection
			if (userLibraryClass instanceof UserLibraryCollectionClassImpl) {
				if (this.isIterator(variable, this.currentStatement, ctx)) {
					this.statementType = StatementType.Iterator;
					this.getIteratorData(variable);
				} else if (this.isOutputBind(variable,
						(UserLibraryCollectionClassImpl) userLibraryClass, ctx)) {
					this.statementType = StatementType.OutputBind;
					this.getOutputBindData(variable, this.currentStatement);
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
				variable.typeName, variable.typeParameterName);
		this.currentIteratorData = new Iterator(variableParameter,
				this.iteratorsAndBinds.size() + this.lastFunctionCount,
				new TokenAddress(this.currentStatement.start,
						this.currentStatement.stop));
	}

	/**
	 * Checks if the expression provided that contains a user library object
	 * corresponds to an output bind that must be translated to the target
	 * runtime.
	 * 
	 * @param ctx
	 *            Statement containing a user library object.
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
			StatementContext stx) {
		List<ExpressionContext> expression = stx.statementExpression()
				.expression().expression();
		if (expression.size() == 2) {
			Symbol destinationSymbol = this.currentScope.getSymbol(expression
					.get(0).getText());
			if (destinationSymbol instanceof VariableSymbol) {
				VariableSymbol destinationVariableSymbol = (VariableSymbol) destinationSymbol;
				Variable destinationVariable = new Variable(
						destinationVariableSymbol.name,
						destinationVariableSymbol.typeName,
						destinationVariableSymbol.typeParameterName);
				Variable userLibraryVariable = new Variable(
						userLibraryVariableSymbol.name,
						userLibraryVariableSymbol.typeName,
						userLibraryVariableSymbol.typeParameterName);
				TokenAddress statementAddress = this
						.getCurrentStatementAddress();
				this.iteratorsAndBinds.add(new OutputBind(userLibraryVariable,
						destinationVariable, this.iteratorsAndBinds.size()
								+ lastFunctionCount, statementAddress));
			}
		}
	}
}
