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
import java.util.List;

import org.antlr.v4.runtime.RuleContext;

import br.ufmg.dcc.parallelme.compiler.antlr.JavaBaseListener;
import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser;
import br.ufmg.dcc.parallelme.compiler.antlr.JavaParser.*;
import br.ufmg.dcc.parallelme.compiler.symboltable.*;
import br.ufmg.dcc.parallelme.compiler.userlibrary.UserLibraryClassFactory;

/**
 * Scope-driven listener. It updates <b>currentScope</b> variable accordingly to
 * the symbol currently being visited.
 * 
 * It may also be used to create the symbol table, once it will fill the root
 * scope provided in the constructor with all the symbols found.
 * 
 * @author Wilson de Carvalho
 */
public class ScopeDrivenListener extends JavaBaseListener {
	// Counts the number of anonymous objects created.
	protected int anonymousObjectsCounter = 0;
	// Counts the number of literals found.
	protected int literalCounter = 0;
	// Current scope.
	protected Symbol currentScope = null;
	// Previos scope.
	protected Symbol previousScope = null;
	// Indicates if a user library class was detected.
	protected boolean userLibraryDetected = false;
	// Stores the current statement to be evaluated separatelly during
	// expression check.
	protected JavaParser.StatementContext currentStatement;

	/**
	 * Constructor.
	 * 
	 * @param rootScope
	 *            Scope that must be used as the root for scopes created during
	 *            the creation of this symbol table.
	 */
	public ScopeDrivenListener(Symbol rootScope) {
		this.currentScope = rootScope;
	}

	/**
	 * Indicates if a user library class was found during the walk on this
	 * listener.
	 * 
	 * @return
	 */
	public boolean getUserLibraryDetected() {
		return this.userLibraryDetected;
	}

	/**
	 * Updates the current scope for the class being visited.
	 */
	@Override
	public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
		String name = ctx.Identifier().getText();
		String typeParameter = "";
		if (ctx.typeParameters() != null && !ctx.typeParameters().isEmpty())
			typeParameter = ctx.typeParameters().typeParameter(0).getText();
		this.newScope(new ClassSymbol(name, typeParameter, this.currentScope,
				new TokenAddress(ctx.getParent().start, ctx.getParent().stop),
				new TokenAddress(ctx.classBody().start, ctx.classBody().stop)));

	}

	/**
	 * Returns the current scope to the previous one.
	 */
	@Override
	public void exitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
		if (this.currentScope instanceof ClassSymbol) {
			this.previousScope = this.currentScope;
			this.currentScope = this.currentScope.enclosingScope;
		}
	}

	/**
	 * Updates the current scope for the method being visited.
	 */
	@Override
	public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
		String name = ctx.Identifier().getText();
		String returnType = "void";
		if (ctx.type() != null)
			returnType = ctx.type().getText();
		ArrayList<Symbol> arguments = new ArrayList<>();
		if (ctx.formalParameters().formalParameterList() != null) {
			for (FormalParameterContext parameter : ctx.formalParameters()
					.formalParameterList().formalParameter()) {
				String argumentName = parameter.variableDeclaratorId()
						.getText();
				String ret[] = this.getTypeData(parameter.type());
				String typeName = ret[0];
				String typeParameter = ret[1];
				Symbol argumentSymbol;
				if (UserLibraryClassFactory.isValidClass(typeName)) {
					argumentSymbol = new UserLibraryVariableSymbol(
							argumentName, typeName, typeParameter,
							this.currentScope, new TokenAddress(
									parameter.start, parameter.stop),
							new TokenAddress(ctx.start, ctx.stop));
				} else {
					argumentSymbol = new VariableSymbol(argumentName, typeName,
							typeParameter, this.currentScope, new TokenAddress(
									parameter.start, parameter.stop),
							new TokenAddress(ctx.start, ctx.stop));
				}
				arguments.add(argumentSymbol);
			}
		}
		MethodSymbol methodSymbol = new MethodSymbol(name, returnType,
				arguments, this.currentScope, new TokenAddress(ctx.start,
						ctx.stop));
		this.newScope(methodSymbol);
		// If this method declaration is inside a creator symbol, we must check
		// if the creator intantiates a user library function and store its
		// body...
		MethodBodySymbol methodBodySymbol = null;
		if (this.currentScope instanceof MethodSymbol
				&& this.currentScope.enclosingScope instanceof CreatorSymbol) {
			CreatorSymbol creatorSymbol = (CreatorSymbol) this.currentScope.enclosingScope;
			boolean foo = UserLibraryClassFactory
					.isValidClass(creatorSymbol.typeName);
			this.userLibraryDetected |= foo;
			// In case this is a valid user library class, we must take the
			// method body to translate it to C in the future.
			if (foo) {
				methodBodySymbol = new MethodBodySymbol(ctx.methodBody()
						.getText(), methodSymbol, new TokenAddress(ctx
						.methodBody().block().start,
						ctx.methodBody().block().stop));
				methodSymbol.addSymbol(methodBodySymbol);
			}
		}
	}

	/**
	 * Returns the current scope to the previous one.
	 */
	@Override
	public void exitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
		if (this.currentScope instanceof MethodSymbol) {
			this.previousScope = this.currentScope;
			this.currentScope = this.currentScope.enclosingScope;
		}
	}

	/**
	 * Detects those statements that uses user library objects.
	 */
	@Override
	public void enterStatement(JavaParser.StatementContext ctx) {
		this.currentStatement = ctx;
	}

	@Override
	public void exitStatement(JavaParser.StatementContext ctx) {
		this.currentStatement = null;
	}

	/**
	 * Update the currentScope variable with a new scope.
	 * 
	 * @param scopeSymbol
	 *            A symbol to associate to the new scope.
	 */
	private void newScope(Symbol scopeSymbol) {
		this.previousScope = this.currentScope;
		if (this.currentScope != null) {
			this.currentScope.addSymbol(scopeSymbol);
			this.currentScope = scopeSymbol;
		} else {
			this.currentScope = scopeSymbol;
		}
	}

	/**
	 * Extract the class and type parameter. It is necessary to emphasize that
	 * this code is prepared to extract a SINGLE parameter type. In case the
	 * User Library is modified to accept more than one parameter type, this
	 * code and the symbol table must be revised.
	 * 
	 * @return A two element array with type name and parametrized type.
	 */
	protected String[] getTypeData(RuleContext ctx) {
		String[] ret = new String[2];
		if (ctx instanceof TypeContext) {
			TypeContext tpx = (TypeContext) ctx;
			if (tpx.primitiveType() == null) {
				ret[0] = tpx.classOrInterfaceType().Identifier(0).getText();
				ret[1] = "";
				if (!tpx.classOrInterfaceType().typeArguments().isEmpty())
					ret[1] = tpx.classOrInterfaceType().typeArguments(0)
							.typeArgument(0).getText();
			} else {
				ret[0] = ctx.getText();
			}
		} else if (ctx instanceof CreatedNameContext) {
			CreatedNameContext cnx = (CreatedNameContext) ctx;
			if (!cnx.Identifier().isEmpty())
				ret[0] = cnx.Identifier(0).getText();
			if (!cnx.typeArgumentsOrDiamond().isEmpty()) {
				TypeArgumentsContext tpx = cnx.typeArgumentsOrDiamond(0)
						.typeArguments();
				if (tpx != null && tpx.typeArgument() != null
						&& !tpx.typeArgument().isEmpty()) {
					ret[1] = tpx.typeArgument(0).getText();
				} else {
					ret[1] = "";
				}
			}
		}
		return ret;
	}

	/**
	 * Manages object creation statements (those that succeed a <b>new</b>
	 * operator).
	 */
	@Override
	public void enterCreator(JavaParser.CreatorContext ctx) {
		// Gets the constructor arguments
		ArrayList<Symbol> arguments = new ArrayList<>();
		if (ctx.classCreatorRest().arguments().expressionList() != null) {
			arguments = this.createSymbols(ctx.classCreatorRest().arguments()
					.expressionList().expression());
		}
		// Gets the variable that will hold this creator (if any)
		if (ctx.parent.parent.parent instanceof VariableDeclaratorContext) {
			VariableDeclaratorContext variableCtx = (VariableDeclaratorContext) ctx.parent.parent.parent;
			String variableName = variableCtx.variableDeclaratorId().getText();
			VariableSymbol variableSymbol = (VariableSymbol) this.currentScope
					.getSymbolUnderScope(variableName, VariableSymbol.class);
			// It may be an user library class, so search again
			if (variableSymbol == null)
				variableSymbol = (VariableSymbol) this.currentScope
						.getSymbolUnderScope(variableName,
								UserLibraryVariableSymbol.class);
			// If the variable was found this is a named object, otherwise it is
			// an anonymous object.
			if (variableSymbol != null) {
				this.previousScope = this.currentScope;
				this.currentScope = variableSymbol;
				if (!ctx.createdName().typeArgumentsOrDiamond().isEmpty()) {
					String[] ret = this.getTypeData(ctx.createdName());
					this.newScope(new CreatorSymbol(variableName, ret[0],
							ret[1], arguments, variableSymbol,
							new TokenAddress(ctx.start, ctx.stop)));
				} else {
					this.newScope(new CreatorSymbol(variableName, ctx
							.createdName().getText(), "", arguments,
							variableSymbol, new TokenAddress(variableCtx.start,
									variableCtx.stop)));
				}
			}
		} else {
			this.anonymousObjectsCounter += 1;
			String[] ret = this.getTypeData(ctx.createdName());
			this.newScope(new CreatorSymbol(
					SymbolTableDefinitions.anonymousObjectPrefix
							+ this.anonymousObjectsCounter, ret[0], ret[1],
					arguments, this.currentScope, new TokenAddress(ctx.start,
							ctx.stop)));
		}
	}

	/**
	 * Create symbols for a list of expressions.
	 * 
	 * @param list
	 *            List of expressions that must be analyzed.
	 * @return Array of symbols detected.
	 */
	private ArrayList<Symbol> createSymbols(List<ExpressionContext> list) {
		ArrayList<Symbol> arguments = new ArrayList<>();
		for (ExpressionContext expression : list) {
			String name = expression.getText();
			Symbol symbol = null;
			if (expression.primary() != null
					&& expression.primary().literal() != null) {
				symbol = this.createLiteralSymbol(expression.primary()
						.literal());
			} else {
				symbol = this.currentScope.getSymbolUnderScope(name);
			}
			arguments.add(symbol);
		}
		return arguments;
	}

	/**
	 * Creates a literal symbol compatible with the symbol table.
	 * 
	 * @param ctx
	 *            The literal context.
	 * @return A literal symbol compatible with the symbol table.
	 */
	private LiteralSymbol<?> createLiteralSymbol(LiteralContext ctx) {
		LiteralSymbol<?> symbol = null;
		this.literalCounter += 1;
		if (ctx.IntegerLiteral() != null)
			symbol = new LiteralIntegerSymbol(
					SymbolTableDefinitions.literalPrefix + this.literalCounter,
					null, Integer.parseInt(ctx.IntegerLiteral().getText()));
		else if (ctx.BooleanLiteral() != null)
			symbol = new LiteralBooleanSymbol(
					SymbolTableDefinitions.literalPrefix + this.literalCounter,
					null, Boolean.parseBoolean(ctx.BooleanLiteral().getText()));
		else if (ctx.FloatingPointLiteral() != null)
			symbol = new LiteralFloatingPointSymbol(
					SymbolTableDefinitions.literalPrefix + this.literalCounter,
					null,
					Float.parseFloat(ctx.FloatingPointLiteral().getText()));
		else if (ctx.StringLiteral() != null)
			symbol = new LiteralStringSymbol(
					SymbolTableDefinitions.literalPrefix + this.literalCounter,
					null, ctx.StringLiteral().getText());
		else if (ctx.CharacterLiteral() != null)
			symbol = new LiteralCharacterSymbol(
					SymbolTableDefinitions.literalPrefix + this.literalCounter,
					null, new Character(ctx.CharacterLiteral().getText()
							.charAt(0)));
		return symbol;
	}

	/**
	 * Returns the current scope to the previous one.
	 */
	@Override
	public void exitCreator(JavaParser.CreatorContext ctx) {
		if (this.currentScope instanceof CreatorSymbol) {
			this.previousScope = this.currentScope;
			this.currentScope = this.currentScope.enclosingScope;
		}
	}

	/**
	 * Extracts variable name, its class name and type parameters and put them
	 * on the symbol table.
	 */
	@Override
	public void enterLocalVariableDeclaration(
			JavaParser.LocalVariableDeclarationContext ctx) {
		// Get variable type and its parametrized type (if any)
		String ret[] = this.getTypeData(ctx.type());
		String variableType = ret[0];
		String typeParameter = ret[1];
		for (VariableDeclaratorContext variable : ctx.variableDeclarators()
				.variableDeclarator()) {
			String variableName = variable.variableDeclaratorId().getText();
			if (UserLibraryClassFactory.isValidClass(variableType)) {
				this.currentScope.addSymbol(new UserLibraryVariableSymbol(
						variableName, variableType, typeParameter,
						this.currentScope, new TokenAddress(variable
								.variableDeclaratorId().start, variable
								.variableDeclaratorId().stop),
						new TokenAddress(ctx.start, ctx.stop)));
			} else {
				this.currentScope.addSymbol(new VariableSymbol(variableName,
						variableType, typeParameter, this.currentScope,
						new TokenAddress(variable.variableDeclaratorId().start,
								variable.variableDeclaratorId().stop),
						new TokenAddress(ctx.start, ctx.stop)));
			}
		}
	}

	/**
	 * Returns the current scope to the previous one only if it was changed on
	 * exitLocalVariableDeclaration method.
	 */
	@Override
	public void exitLocalVariableDeclaration(
			JavaParser.LocalVariableDeclarationContext ctx) {
		if (this.currentScope instanceof VariableSymbol) {
			this.previousScope = this.currentScope;
			this.currentScope = this.currentScope.enclosingScope;
		}
	}
}
