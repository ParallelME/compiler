/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.compiler;

import static org.junit.Assert.*;

import org.junit.Test;

import org.parallelme.compiler.CompilerArgsVerification.CompilerParameters;
import org.parallelme.compiler.CompilerArgsVerification.TargetRuntime;

/**
 * Performs all tests to validate CompilerArgsVerification class and simulate
 * different possibilities of argument input to the compiler.
 * 
 * @author Wilson de Carvalho
 */
public class CompilerArgsVerificationTest {
	/**
	 * Testing valid arguments.
	 */
	@Test
	public void checkValidArgs() throws Exception {
		String args[] = new String[5];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		args[4] = "-pm";
		this.assertParameters((new CompilerArgsVerification()).checkArgs(args),
				destinationFolder, file, TargetRuntime.ParallelME);
		args[4] = "-rs";
		this.assertParameters((new CompilerArgsVerification()).checkArgs(args),
				destinationFolder, file, TargetRuntime.RenderScript);
		// Inverting order to test if file and destination folder parameters are
		// handled correctly.
		args[0] = "-pm";
		args[1] = "-f";
		args[2] = file;
		args[3] = "-o";
		args[4] = destinationFolder;
		this.assertParameters((new CompilerArgsVerification()).checkArgs(args),
				destinationFolder, file, TargetRuntime.ParallelME);
		args[0] = "-o";
		args[1] = destinationFolder;
		args[2] = "-pm";
		args[3] = "-f";
		args[4] = file;
		this.assertParameters((new CompilerArgsVerification()).checkArgs(args),
				destinationFolder, file, TargetRuntime.ParallelME);
	}

	private void assertParameters(CompilerParameters cp,
			String destinationFolder, String file, TargetRuntime runtime) {
		assertEquals(destinationFolder, cp.destinationFolder);
		assertEquals(1, cp.files.length);
		assertEquals(file, cp.files[0]);
		assertEquals(runtime, cp.targetRuntime);
	}

	/**
	 * Testing mixed valid and invalid arguments.
	 */
	@Test
	public void checkInvalidArgs() throws Exception {
		String args[] = new String[5];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		args[4] = "$";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = "-f";
		args[1] = file;
		args[2] = "$";
		args[3] = destinationFolder;
		args[4] = "-pm";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = "$";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		args[4] = "-pm";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = "$";
		args[1] = file;
		args[2] = "$";
		args[3] = destinationFolder;
		args[4] = "$";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
	}

	/**
	 * Testing insuficient, but valid arguments.
	 */
	@Test
	public void checkInsuficientArgs() throws Exception {
		String args[] = new String[4];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args = new String[3];
		args[0] = "-f";
		args[1] = file;
		args[2] = "-pm";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args = new String[2];
		args[0] = "-f";
		args[1] = file;
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args = new String[1];
		args[0] = "-pm";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = "-rs";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
	}

	/**
	 * Testing invalid input file.
	 */
	@Test(expected = Exception.class)
	public void checkInvalidInputFile() throws Exception {
		String args[] = new String[5];
		String file = "../NotAValidFileName$.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		args[4] = "-pm";
		(new CompilerArgsVerification()).checkArgs(args);
	}

	/**
	 * Testing invalid output folder.
	 */
	@Test(expected = Exception.class)
	public void checkInvalidOutputFolder() throws Exception {
		String args[] = new String[5];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./NotAValidFolderName$";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		args[4] = "-pm";
		(new CompilerArgsVerification()).checkArgs(args);
	}
}
