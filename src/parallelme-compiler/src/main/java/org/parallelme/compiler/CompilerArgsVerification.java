/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import java.io.File;
import java.util.ArrayList;

/**
 * Class responsible for input argument verification.
 * 
 * @author Wilson de Carvalho
 */
public class CompilerArgsVerification {
	public class CompilerParameters {
		public String[] files;
		public String destinationFolder;
	}

	public CompilerParameters checkArgs(String[] args) throws Exception {
		CompilerParameters parameters = new CompilerParameters();
		boolean filesFound, destinationFolderFound;
		filesFound = destinationFolderFound = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-f")) {
				if (filesFound)
					throw new Exception("Duplicated argument: -f");
				if (i < args.length - 1) {
					String filesParam = args[++i];
					if (filesParam.startsWith("-"))
						throw new Exception("Invalid file: " + filesParam);
					// Remove quotes
					String files = filesParam.replace('\"', ' ').trim();
					parameters.files = checkInputFilesArg(files);
					filesFound = true;
				}
			}
			if (args[i].equals("-o")) {
				if (i < args.length - 1) {
					if (destinationFolderFound)
						throw new Exception("Duplicated argument: -o");
					String filesParam = args[++i];
					if (filesParam.startsWith("-"))
						throw new Exception("Invalid file: " + filesParam);
					// Remove quotes
					String destinationFolder = filesParam.replace('\"', ' ')
							.trim();
					parameters.destinationFolder = checkOutputFilesArg(destinationFolder);
					destinationFolderFound = true;
				}
			}
		}
		if (filesFound && destinationFolderFound)
			return parameters;
		else
			return null;
	}

	/**
	 * List all java files on the directory informed. If a single file is
	 * informed, check if it is a java file and returns it if ok.
	 * 
	 * @param arg
	 *            Argument that corresponds to input files.
	 * @return A array of strings with file names. Empty if no Java file was
	 *         found on the path informed.
	 * @throws Exception
	 */
	private String[] checkInputFilesArg(String arg) throws Exception {
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
					throw new Exception(
							"ERROR => Invalid input file or directory: "
									+ file.getName());
				}
			}
		} else {
			File file = new File(arg);
			if (file.exists() && file.isFile()
					&& file.getName().endsWith(".java")) {
				files.add(arg);
			} else {
				throw new Exception(
						"ERROR => Invalid input file or directory: "
								+ file.getName());
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
	 * @throws Exception
	 */
	private String checkOutputFilesArg(String arg) throws Exception {
		File file = new File(arg);
		if (file.exists() && file.isDirectory()) {
			return arg;
		} else {
			throw new Exception("ERROR => Invalid output directory: "
					+ file.getName());
		}
	}
}
