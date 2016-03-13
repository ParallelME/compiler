/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / _ / /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.compiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import br.ufmg.dcc.parallelme.compiler.runtime.RenderScriptRuntimeDefinition;
import br.ufmg.dcc.parallelme.compiler.runtime.translation.SimpleTranslator;

/**
 * Main file for calling ParallelME compiler.
 * 
 * @author Wilson de Carvalho
 */
public class Main {

	public static void main(String[] args) {
		try {
			if (args.length == 4) {
				String[] inputInfo = null;
				String outputInfo = null;
				if (args[0].trim().startsWith("-f")
						&& args[2].trim().startsWith("-o")) {
					inputInfo = checkInputFilesArg(args[1]);
					outputInfo = checkOutputFilesArg(args[3]);
				} else if (args[2].trim().startsWith("-f")
						&& args[0].trim().startsWith("-o")) {
					inputInfo = checkInputFilesArg(args[3]);
					outputInfo = checkOutputFilesArg(args[1]);
				}
				if (inputInfo != null && inputInfo.length > 0
						&& outputInfo != null) {
					SimpleLogger.logError = true;
					SimpleLogger.logInfo = true;
					(new Compiler(new RenderScriptRuntimeDefinition(new SimpleTranslator())))
							.compile(inputInfo, outputInfo);
				} else {
					printHelpMsg();
				}
			} else {
				printHelpMsg();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * List all java files on the directory informed. If a single file is
	 * informed, check if it is a java file and returns it if ok.
	 * 
	 * @param arg
	 *            Argument that corresponds to input files.
	 * @return A array of strings with file names. Empty if no Java file was
	 *         found on the path informed.
	 */
	private static String[] checkInputFilesArg(String arg) {
		ArrayList<String> files = new ArrayList<>();
		if (arg.contains(";")) {
			String foo[] = arg.split(";");
			for (int i = 0; i < foo.length; i++) {
				File file = new File(foo[i]);
				if (file.exists()) {
					if (file.getName().endsWith(".java")) {
						files.add(foo[i]);
					} else if (file.isDirectory()) {
						for (String fileName : file.list()) {
							File bar = new File(fileName);
							if (bar.isFile() && bar.getName().endsWith(".java")) {
								files.add(fileName);
							}
						}
					}
				} else {
					System.out
							.println("ERROR => Invalid input file or directory: "
									+ file.getName());
				}
			}
		} else {
			File file = new File(arg);
			if (file.exists() && file.isFile()
					&& file.getName().endsWith(".java")) {
				files.add(arg);
			}
		}
		String[] ret = new String[files.size()];
		return files.toArray(ret);
	}

	/**
	 * Checks if an informed output directory is valid.
	 * 
	 * @param arg
	 *            Argument that corresponds to output directory.
	 * @return String containing the output directory if it is valid. Null
	 *         otherwise.
	 */
	private static String checkOutputFilesArg(String arg) {
		File file = new File(arg);
		if (file.exists() && file.isDirectory()) {
			return arg;
		} else {
			System.out.println("ERROR => Invalid output directory: "
					+ file.getName());
			return null;
		}
	}

	private static void printHelpMsg() {
		System.out
				.println("ParallelME compiler accepts the following arguments:");
		System.out
				.println("-f\tJava file or directory path. In case of multiple files, use ");
		System.out
				.println("\ta ; separated list of files or directories without space.");
		System.out.println("-o\tOutput directory path.");
	}
}