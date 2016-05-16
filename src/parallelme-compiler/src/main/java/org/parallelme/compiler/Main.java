/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import org.parallelme.compiler.CompilerArgsVerification.CompilerParameters;
import org.parallelme.compiler.RuntimeDefinition.TargetRuntime;
import org.parallelme.compiler.exception.CompilationException;

/**
 * Main file for calling ParallelME compiler.
 * 
 * @author Wilson de Carvalho
 */
public class Main {

	public static void main(String[] args) {
		try {
			CompilerParameters parameters = null;
			try {
				parameters = (new CompilerArgsVerification()).checkArgs(args);
			} catch (Exception e) {
				printHelpMsg();
			}
			if (parameters != null) {
				SimpleLogger.logError = true;
				SimpleLogger.logInfo = true;
				SimpleLogger.logWarn = true;
				if (parameters.targetRuntime == TargetRuntime.ParallelME) {
					(new Compiler()).compile(parameters.files,
							parameters.destinationFolder);
				} else {
					(new Compiler()).compile(parameters.files,
							parameters.destinationFolder);
				}
			} else {
				printHelpMsg();
			}
		} catch (CompilationException ex) {
			SimpleLogger.error(ex.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void printHelpMsg() {
		System.out
				.println("ParallelME compiler accepts the following arguments:");
		System.out
				.println("-f\t\tJava file or directory path (quoted). In case of multiple files,");
		System.out
				.println("\t\tuse a ; separated list of files or directories in the same quoted string.");
		System.out.println("-o\t\tOutput directory path (quoted).");
		System.out
				.println("-rs or -pm\tRenderScript (-rs) or ParallelME (-pm) runtimes.");
		System.out.println("\t\tOnly one runtime is allowed per call.");
	}
}