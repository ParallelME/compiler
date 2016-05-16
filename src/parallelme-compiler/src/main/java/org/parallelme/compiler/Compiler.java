/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.io.FileInputStream;
import java.io.IOException;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.parallelme.compiler.antlr.JavaLexer;
import org.parallelme.compiler.antlr.JavaParser;
import org.parallelme.compiler.exception.CompilationException;
import org.parallelme.compiler.symboltable.*;
import org.parallelme.compiler.translation.SimpleTranslator;

/**
 * Main class for ParallelME compiler. It is responsible for parsing the user
 * code in Java, detect ParallelME usage and translate it into an output code
 * that can execute on the target runtime.
 * 
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public class Compiler {
	/**
	 * Compile a list of files storing them on the folder informed.
	 * 
	 * @param files
	 *            List of files that must be compiled.
	 * @param destinationFolder
	 *            Destination folder.
	 * @throws IOException
	 *             Exception thrown in case of issue while reading or writing
	 *             files.
	 */
	public void compile(String[] files, String destinationFolder)
			throws IOException, CompilationException {
		RootSymbol[] symbolTables = new RootSymbol[files.length];
		ParseTree[] parseTrees = new ParseTree[files.length];
		JavaParser[] javaParser = new JavaParser[files.length];
		TokenStreamRewriter[] tokenStreams = new TokenStreamRewriter[files.length];
		ParseTreeWalker walker = new ParseTreeWalker();
		// ####### First pass #######
		for (int i = 0; i < files.length; i++) {
			String file = files[i];
			SimpleLogger.info("1st pass file - " + file);
			FileInputStream is = new FileInputStream(file);
			try {
				CommonTokenStream tokenStream = new CommonTokenStream(
						new JavaLexer(new ANTLRInputStream(is)));
				JavaParser parser = new JavaParser(tokenStream);
				ParseTree tree = parser.compilationUnit();
				// Object that will be used to rewrite this file, if necessary
				TokenStreamRewriter tokenStreamRewriter = new TokenStreamRewriter(
						tokenStream);
				// Root of this file's symbol table
				RootSymbol symbolTable = new RootSymbol();
				CompilerFirstPassListener listener = new CompilerFirstPassListener(
						symbolTable);
				// Walk on the parse tree
				walker.walk(listener, tree);
				// Stores objects by file for next compiler pass
				tokenStreams[i] = tokenStreamRewriter;
				javaParser[i] = parser;
				parseTrees[i] = tree;
				symbolTables[i] = symbolTable;
			} finally {
				is.close();
			}
		}
		// ####### Second pass and code translation #######
		CompilerCodeTranslator codeTranslator = new CompilerCodeTranslator(
				destinationFolder, new SimpleTranslator());
		for (int i = 0; i < files.length; i++) {
			String file = files[i];
			SimpleLogger.info("2nd pass file - " + file);
			TokenStreamRewriter tokenStreamRewriter = tokenStreams[i];
			// Walk on the parse tree again
			CompilerSecondPassListener listener = new CompilerSecondPassListener(
					tokenStreamRewriter.getTokenStream());
			walker.walk(listener, parseTrees[i]);
			codeTranslator.run(symbolTables[i], listener, tokenStreamRewriter);
		}
	}
}
