/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme;

import android.graphics.Bitmap;

/**
 * Wrapper class for JNI calls to ParallelME runtime. It is also responsible for
 * keeping the runtime pointer in memory so it can be used in multiple calls to
 * the low-level runtime without recreating objects.
 *
 * @author Pedro Caldeira, Wilson de Carvalho
 */
public class ParallelMERuntime {
	private static final ParallelMERuntime instance = new ParallelMERuntime();
	public final long runtimePointer;

	public static ParallelMERuntime getInstance() {
		return instance;
	}

	private native long nativeInit();

	private native void nativeCleanUpRuntime(long runtimePointer);

	private native void nativeCleanUpImage(long imagePointer);

	private native void nativeCleanUpArray(long arrayPointer);

	private native long nativeCreateHDRImage(long runtimePointer, byte[] data,
			int width, int height);

	private native void nativeToBitmapHDRImage(long runtimePointer,
			long imagePointer, Bitmap bitmap);

	private native int nativeGetHeight(long imagePointer);

	private native int nativeGetWidth(long imagePointer);

	private ParallelMERuntime() {
		System.loadLibrary("ParallelME");
		this.runtimePointer = nativeInit();
	}

	@Override
	protected void finalize() throws Throwable {
		nativeCleanUpRuntime(runtimePointer);
		super.finalize();
	}

	public void cleanUpImage(long imagePointer) {
		nativeCleanUpImage(imagePointer);
	}

	public void cleanUpArray(long arrayPointer) {
		nativeCleanUpArray(arrayPointer);
	}

	public long createHDRImage(byte[] data, int width, int height) {
		return nativeCreateHDRImage(runtimePointer, data, width, height);
	}

	public void toBitmapHDRImage(long imagePointer, Bitmap bitmap) {
		nativeToBitmapHDRImage(runtimePointer, imagePointer, bitmap);
	}

	public int getHeight(long imagePointer) {
		return nativeGetHeight(imagePointer);
	}

	public int getWidth(long imagePointer) {
		return nativeGetWidth(imagePointer);
	}
}