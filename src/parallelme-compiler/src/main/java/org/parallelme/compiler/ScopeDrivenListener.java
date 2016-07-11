/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.RuleContext;
import org.parallelme.compiler.antlr.JavaBaseListener;
import org.parallelme.compiler.antlr.JavaParser;
import org.parallelme.compiler.antlr.JavaParser.*;
import org.parallelme.compiler.symboltable.*;
import org.parallelme.compiler.userlibrary.UserLibraryClassFactory;
import org.parallelme.compiler.util.Pair;
import org.parallelme.compiler.util.StringUtil;

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
	protected int symbolsCounter = 1;
	// Current scope.
	protected Symbol currentScope = null;
	// Previos scope.
	protected Symbol previousScope = null;
	// Indicates if a user library class was detected.
	protected boolean userLibraryDetected = false;
	// Stores the current statement to be evaluated separatelly during
	// expression check.
	protected JavaParser.StatementContext currentStatement;
	protected JavaParser.LocalVariableDeclarationStatementContext currentVariableStatement;

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
				new TokenAddress(ctx.classBody().start, ctx.classBody().stop),
				this.symbolsCounter++));

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
				Pair<String, List<String>> ret = this.getTypeData(parameter
						.type());
				String typeName = ret.left;
				List<String> typeParameters = ret.right;
				String modifier = "";
				if (!parameter.variableModifier().isEmpty())
					modifier = parameter.variableModifier(0).getText();
				Symbol argumentSymbol;
				if (UserLibraryClassFactory.isValidClass(typeName)) {
					argumentSymbol = new UserLibraryVariableSymbol(
							argumentName, typeName, typeParameters, modifier,
							this.currentScope, new TokenAddress(
									parameter.start, parameter.stop),
							new TokenAddress(ctx.start, ctx.stop),
							this.symbolsCounter++);
				} else {
					argumentSymbol = new VariableSymbol(argumentName, typeName,
							typeParameters, modifier, this.currentScope,
							new TokenAddress(parameter.start, parameter.stop),
							new TokenAddress(ctx.start, ctx.stop),
							this.symbolsCounter++);
				}
				arguments.add(argumentSymbol);
			}
		}
		MethodSymbol methodSymbol = new MethodSymbol(name, returnType,
				arguments, this.currentScope, new TokenAddress(ctx.start,
						ctx.stop), this.symbolsCounter++);
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
						ctx.methodBody().block().stop), this.symbolsCounter++);
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

	@Override
	public void enterStatement(JavaParser.StatementContext ctx) {
		this.currentStatement = ctx;
	}

	@Override
	public void exitStatement(JavaParser.StatementContext ctx) {
		this.currentStatement = null;
	}

	@Override
	public void enterLocalVariableDeclarationStatement(
			JavaParser.LocalVariableDeclarationStatementContext ctx) {
		this.currentVariableStatement = ctx;
	}

	@Override
	public void exitLocalVariableDeclarationStatement(
			JavaParser.LocalVariableDeclarationStatementContext ctx) {
		this.currentVariableStatement = null;
	}

	/**
	 * Creates a new token for the address of the current statement. Accordingly
	 * to ANTLR structure, a statement can be stored on several different
	 * classes, so instances of these classes are stored in objects during the
	 * walk and filtered here, since only one will exist at a time.
	 * 
	 * @return Statement address. Null if no statement is currently being
	 *         visited.
	 */
	protected TokenAddress getCurrentStatementAddress() {
		TokenAddress ret = null;
		if (this.currentStatement != null) {
			ret = new TokenAddress(this.currentStatement.start,
					this.currentStatement.stop);
		} else if (this.currentVariableStatement != null) {
			ret = new TokenAddress(this.currentVariableStatement.start,
					this.currentVariableStatement.stop);
		}
		return ret;
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
	 * Extract the class and type parameters.
	 * 
	 * @param ctx
	 *            Rule context.
	 * 
	 * @return A pair with type name and a list of parametrized types.
	 */
	protected Pair<String, List<String>> getTypeData(RuleContext ctx) {
		String typeName = "";
		List<String> parameterizedType = null;
		if (ctx instanceof TypeContext) {
			TypeContext tpx = (TypeContext) ctx;
			if (tpx.primitiveType() == null) {
				// In case the class type contains package description,
				// concatenates it in a single string
				typeName = StringUtil.mkString(tpx.classOrInterfaceType()
						.Identifier().toArray(), ".");
				parameterizedType = new ArrayList<>();
				for (TypeArgumentsContext typeArgument : tpx
						.classOrInterfaceType().typeArguments()) {
					parameterizedType.add(typeArgument.typeArgument(0)
							.getText());
				}
			} else {
				typeName = ctx.getText();
			}
		} else if (ctx instanceof CreatedNameContext) {
			CreatedNameContext cnx = (CreatedNameContext) ctx;
			if (!cnx.Identifier().isEmpty())
				typeName = cnx.Identifier(0).getText();
			if (!cnx.typeArgumentsOrDiamond().isEmpty()) {
				TypeArgumentsOrDiamondContext typeOrDiamond = cnx
						.typeArgumentsOrDiamond().get(0);
				TypeArgumentsContext tpx = typeOrDiamond.typeArguments();
				if (tpx != null && tpx.typeArgument() != null
						&& !tpx.typeArgument().isEmpty()) {
					parameterizedType = new ArrayList<>();
					for (TypeArgumentContext typeArgument : tpx.typeArgument())
						parameterizedType.add(typeArgument.getText());
				}
			}
		} else if (ctx instanceof ClassOrInterfaceTypeContext) {
			ClassOrInterfaceTypeContext cix = (ClassOrInterfaceTypeContext) ctx;
			typeName = cix.getText();
		}
		return new Pair<String, List<String>>(typeName, parameterizedType);
	}

	/**
	 * Manages object creation statements (those that succeed a <b>new</b>
	 * operator).
	 * 
	 * @param ctx
	 *            Creator context.
	 */
	@Override
	public void enterCreator(JavaParser.CreatorContext ctx) {
		// Gets the constructor arguments
		ArrayList<Symbol> arguments = new ArrayList<>();
		if (ctx.classCreatorRest() != null
				&& ctx.classCreatorRest().arguments().expressionList() != null) {
			arguments = this.createSymbols(ctx.classCreatorRest().arguments()
					.expressionList().expression());
		}
		if (ctx.parent.parent.parent instanceof VariableDeclaratorContext) {
			// Case 1: ClassName objectName = new ClassName(p1, p2, ..., pn);
			VariableDeclaratorContext variableCtx = (VariableDeclaratorContext) ctx.parent.parent.parent;
			String variableName = variableCtx.variableDeclaratorId().getText();
			this.createCreatorSymbol(ctx, variableName, arguments);
		} else if (ctx.parent.parent instanceof ExpressionContext) {
			// Case 2:
			// objectName = new ClassName(p1, p2, ..., pn);
			// OR
			// this.objectName = new ClassName(p1, p2, ..., pn);
			// OR
			// ANYTHING.objectName = new ClassName(p1, p2, ..., pn);
			ExpressionContext expressionCtx = (ExpressionContext) ctx.parent.parent;
			this.creatorSymbolCase2(ctx, expressionCtx, arguments);
		} else {
			// Case 3:
			// someObject.method(new ClassName(p1, p2, ..., pn));
			// OR
			// someObject.method(new ClassName(p1, p2, ..., pn) { ... });
			this.symbolsCounter++;
			Pair<String, List<String>> ret = this
					.getTypeData(ctx.createdName());
			this.newScope(new CreatorSymbol(
					SymbolTableDefinitions.anonymousObjectPrefix
							+ this.symbolsCounter, "", ret.left, ret.right,
					arguments, this.currentScope, new TokenAddress(ctx.start,
							ctx.stop), this.getCurrentStatementAddress(),
					this.symbolsCounter));
		}
	}

	/**
	 * Extracts creator information for cases like:
	 * 
	 * objectName = new ClassName(p1, p2, ..., pn);<br>
	 * OR <br>
	 * this.objectName = new ClassName(p1, p2, ..., pn); <br>
	 * OR <br>
	 * ANYTHING.objectName = new ClassName(p1, p2, ..., pn);
	 */
	private void creatorSymbolCase2(JavaParser.CreatorContext creatorCtx,
			ExpressionContext expressionCtx, List<Symbol> arguments) {
		List<ExpressionContext> expressions = expressionCtx.expression();
		// Checks if is case of "expression = expression"
		if (!expressions.isEmpty() && expressions.size() == 2) {
			ExpressionContext foo = expressions.get(0);
			// Checks case "objectName = new ClassName(p1, p2, ..., pn);"
			if (foo.primary() != null) {
				String variableName = foo.primary().getText();
				this.createCreatorSymbol(creatorCtx, variableName, arguments);
			} else if (foo.Identifier() != null) {
				// Check cases:
				// this.objectName = new ClassName(p1, p2, ..., pn);
				// OR
				// ANYTHING.objectName = new ClassName(p1, p2, ..., pn);
				String variableName = foo.Identifier().getText();
				this.createCreatorSymbol(creatorCtx, variableName, arguments);
			}
		}
	}

	/**
	 * To avoid code duplication, we create a creator symbol (wow, this is
	 * funny) here.
	 */
	private void createCreatorSymbol(JavaParser.CreatorContext creatorCtx,
			String variableName, List<Symbol> arguments) {
		if (!creatorCtx.createdName().typeArgumentsOrDiamond().isEmpty()) {
			Pair<String, List<String>> ret = this.getTypeData(creatorCtx
					.createdName());
			this.newScope(new CreatorSymbol("PM_" + variableName + "Creator",
					variableName, ret.left, ret.right, arguments,
					this.currentScope, new TokenAddress(creatorCtx.start,
							creatorCtx.stop),
					this.getCurrentStatementAddress(), this.symbolsCounter++));
		} else {
			this.newScope(new CreatorSymbol("PM_" + variableName + "Creator",
					variableName, creatorCtx.createdName().getText(),
					new ArrayList<String>(), arguments, this.currentScope,
					new TokenAddress(creatorCtx.start, creatorCtx.stop), this
							.getCurrentStatementAddress(),
					this.symbolsCounter++));
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
			String expressionText = expression.getText();
			Symbol symbol = null;
			if (expression.primary() != null
					&& expression.primary().literal() != null) {
				symbol = this.createLiteralSymbol(expression.primary()
						.literal());
			} else if (!expression.expression().isEmpty()
					|| (expression.primary() != null
							&& expression.primary().type() != null && expression
							.primary().type().classOrInterfaceType() != null)) {
				symbol = new ExpressionSymbol(expressionText,
						this.currentScope, new TokenAddress(expression.start,
								expression.stop), this.symbolsCounter++);
			} else {
				symbol = this.currentScope.getSymbolUnderScope(expressionText);
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
		this.symbolsCounter++;
		if (ctx.IntegerLiteral() != null)
			symbol = new LiteralIntegerSymbol(
					SymbolTableDefinitions.literalPrefix + this.symbolsCounter,
					null, Integer.parseInt(ctx.IntegerLiteral().getText()),
					this.symbolsCounter);
		else if (ctx.BooleanLiteral() != null)
			symbol = new LiteralBooleanSymbol(
					SymbolTableDefinitions.literalPrefix + this.symbolsCounter,
					null, Boolean.parseBoolean(ctx.BooleanLiteral().getText()),
					this.symbolsCounter);
		else if (ctx.FloatingPointLiteral() != null)
			symbol = new LiteralFloatingPointSymbol(
					SymbolTableDefinitions.literalPrefix + this.symbolsCounter,
					null,
					Float.parseFloat(ctx.FloatingPointLiteral().getText()),
					this.symbolsCounter);
		else if (ctx.StringLiteral() != null)
			symbol = new LiteralStringSymbol(
					SymbolTableDefinitions.literalPrefix + this.symbolsCounter,
					null, ctx.StringLiteral().getText(), this.symbolsCounter);
		else if (ctx.CharacterLiteral() != null)
			symbol = new LiteralCharacterSymbol(
					SymbolTableDefinitions.literalPrefix + this.symbolsCounter,
					null, new Character(ctx.CharacterLiteral().getText()
							.charAt(0)), this.symbolsCounter);
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
		this.createVariable(ctx.variableDeclarators(), ctx.type(),
				ctx.variableModifier(), this.getCurrentStatementAddress());
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

	@Override
	public void enterFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
		if (ctx.parent.parent instanceof ClassBodyDeclarationContext) {
			ClassBodyDeclarationContext cbdctx = (ClassBodyDeclarationContext) ctx.parent.parent;
			this.createVariable(ctx.variableDeclarators(), ctx.type(),
					new ArrayList<VariableModifierContext>(), new TokenAddress(
							cbdctx.start, cbdctx.stop));
		} else {
			throw new RuntimeException("Unsupported field declaration: "
					+ ctx.getText());
		}
	}

	/**
	 * Creates variable symbol for local variables and field declarations.
	 * 
	 * @param variablesCtx
	 *            Variable declarator context.
	 * @param typeCtx
	 *            Type context.
	 * @param variableModifiers
	 *            List of variable modifiers for current variable.
	 * @param statementAddress
	 *            Token address for the variable's statement.
	 */
	public void createVariable(VariableDeclaratorsContext variablesCtx,
			TypeContext typeCtx,
			List<VariableModifierContext> variableModifiers,
			TokenAddress statementAddress) {
		Pair<String, List<String>> ret = this.getTypeData(typeCtx);
		String variableType = ret.left;
		List<String> typeParameters = ret.right;
		for (VariableDeclaratorContext variable : variablesCtx
				.variableDeclarator()) {
			String variableName = variable.variableDeclaratorId().getText();
			String modifier = variableModifiers.isEmpty() ? ""
					: variableModifiers.get(0).getText();
			if (UserLibraryClassFactory.isValidClass(variableType)) {
				this.currentScope.addSymbol(new UserLibraryVariableSymbol(
						variableName, variableType, typeParameters, modifier,
						this.currentScope, new TokenAddress(variable
								.variableDeclaratorId().start, variable
								.variableDeclaratorId().stop),
						statementAddress, this.symbolsCounter++));
			} else {
				this.currentScope.addSymbol(new VariableSymbol(variableName,
						variableType, typeParameters, modifier,
						this.currentScope, new TokenAddress(variable
								.variableDeclaratorId().start, variable
								.variableDeclaratorId().stop),
						statementAddress, this.symbolsCounter++));
			}
		}
	}
}
