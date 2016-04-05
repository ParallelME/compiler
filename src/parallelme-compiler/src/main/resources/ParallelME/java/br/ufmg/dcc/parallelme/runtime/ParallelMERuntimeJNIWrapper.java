/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 *  DCC-UFMG
 */

package br.ufmg.dcc.parallelme.runtime;

/**
 * Wrapper class for JNI calls to ParallelME runtime. It is also
 * rensponsible for keeping the runtime pointer in memory so it
 * can be used in multiple calls to the low-level runtime without
 * recreating objects.
 *
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public final class ParallelMERuntimeJNIWrapper {
	private static final ParallelMERuntimeJNIWrapper instance = new ParallelMERuntimeJNIWrapper();
	public final long runtimePointer = init();

	public static ParallelMERuntimeJNIWrapper getInstance() {
		return this.instance;
	}

	private native long init(long runtimePointer);
	private native void cleanUp(long runtimePointer);

	@Override
	protected void finalize throws Throwable {
		cleanUp(runtimePointer);
	}

	static {
		System.loadLibrary("Name_yet_to_be_defined");
	}
}
