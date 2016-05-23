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
		String args[] = new String[4];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		this.assertParameters((new CompilerArgsVerification()).checkArgs(args),
				destinationFolder, file);
		// Inverting order to test if file and destination folder parameters are
		// handled correctly.
		args[0] = "-o";
		args[1] = destinationFolder;
		args[2] = "-f";
		args[3] = file;
		this.assertParameters((new CompilerArgsVerification()).checkArgs(args),
				destinationFolder, file);
	}

	private void assertParameters(CompilerParameters cp,
			String destinationFolder, String file) {
		assertEquals(destinationFolder, cp.destinationFolder);
		assertEquals(1, cp.files.length);
		assertEquals(file, cp.files[0]);
	}

	/**
	 * Testing mixed valid and invalid arguments.
	 */
	@Test
	public void checkInvalidArgs() throws Exception {
		String args[] = new String[4];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-TST";
		args[3] = destinationFolder;
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = "-TST";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = "-TST";
		args[1] = file;
		args[2] = "-TST";
		args[3] = destinationFolder;
		assertNull((new CompilerArgsVerification()).checkArgs(args));
	}

	@Test(expected = Exception.class)
	public void checkInvalidPath() throws Exception {
		String args[] = new String[4];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = "-o";
		args[2] = file;
		args[3] = destinationFolder;
		(new CompilerArgsVerification()).checkArgs(args);
	}

	/**
	 * Testing insuficient, but valid arguments.
	 */
	@Test
	public void checkInsuficientArgs() throws Exception {
		String args[] = new String[3];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = file;
		args[2] = destinationFolder;
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = file;
		args[1] = "-o";
		args[2] = destinationFolder;
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args = new String[2];
		args[0] = "-f";
		args[1] = file;
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args = new String[1];
		args[0] = "-o";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
		args[0] = "-f";
		assertNull((new CompilerArgsVerification()).checkArgs(args));
	}

	/**
	 * Testing invalid input file.
	 */
	@Test(expected = Exception.class)
	public void checkInvalidInputFile() throws Exception {
		String args[] = new String[4];
		String file = "../NotAValidFileNamePM_.java";
		String destinationFolder = "./";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		(new CompilerArgsVerification()).checkArgs(args);
	}

	/**
	 * Testing invalid output folder.
	 */
	@Test(expected = Exception.class)
	public void checkInvalidOutputFolder() throws Exception {
		String args[] = new String[4];
		String file = "../samples/BitmapLoaderTest.java";
		String destinationFolder = "./NotAValidFolderNamePM_";
		args[0] = "-f";
		args[1] = file;
		args[2] = "-o";
		args[3] = destinationFolder;
		(new CompilerArgsVerification()).checkArgs(args);
	}
}
