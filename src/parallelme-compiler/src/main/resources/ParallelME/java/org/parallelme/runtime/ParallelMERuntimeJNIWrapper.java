/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.runtime;

import android.graphics.Bitmap;

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

	private native long init();
	private native void cleanUp(long runtimePointer);
	private native void waitFinish(long runtimePointer);
	private native void toFloat(long runtimePointer, int inputBufferId, int outputBufferId, int worksize);
	private native void toBitmap(long runtimePointer, int inputBufferId, int outputBufferId, int worksize);
	private native int createByteAllocation(long runtimePointer, byte[] data, int elements);
	private native int createFloatAllocation(long runtimePointer, float[] data, int elements);
	private native int createBitmapAllocation(long runtimePointer, Bitmap data, int elements);

	@Override
	protected void finalize throws Throwable {
		cleanUp(runtimePointer);
	}

	public void waitFinish() {
        waitFinish(runtimePointer);
    }

	static {
		System.loadLibrary("reinhardOpenCLOperator");
	}

	public void toFloat(long runtimePointer, int inputBufferId, int outputBufferId, int worksize) {
		return toFloat(runtimePointer, inputBufferId, outputBufferId, worksize);
	}

	public void toBitmap(long runtimePointer, int inputBufferId, int outputBufferId, int worksize) {
		return toBitmap(runtimePointer, inputBufferId, outputBufferId, worksize);
	}

	public int createAllocation(byte[] data, int elements) {
		return createByteAllocation(runtimePointer, data, elements);
	}

	public int createAllocation(float[] data, int elements) {
		return createFloatAllocation(runtimePointer, data, elements);
	}

	public int createAllocation(Bitmap data, int elements) {
		return createBitmapAllocation(runtimePointer, data, elements);
	}
}
