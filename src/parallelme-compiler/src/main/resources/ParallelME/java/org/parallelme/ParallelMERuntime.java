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
	private native long nativeCreateShortArray(long runtimePointer, short[] array, int length);
	private native long nativeCreateIntArray(long runtimePointer, int[] array, int length);
	private native long nativeCreateFloatArray(long runtimePointer, float[] array, int length);
	private native void nativeToArray(long runtimePointer, long arrayPointer, short[]);
	private native void nativeToArray(long runtimePointer, long arrayPointer, int[]);
	private native void nativeToArray(long runtimePointer, long arrayPointer, float[]);
	private native long nativeCreateBitmapImage(long runtimePointer, Bitmap bitmap, int width, int height);
	private native void nativeToBitmapBitmapImage(long runtimePointer, long imagePointer, Bitmap bitmap);
	private native long nativeCreateHDRImage(long runtimePointer, byte[] data, int width, int height);
	private native void nativeToBitmapHDRImage(long runtimePointer, long imagePointer, Bitmap bitmap);
	private native int nativeGetHeight(long imagePointer);
	private native int nativeGetWidth(long imagePointer);
	private native int nativeGetLength(long arrayPointer);

	private ParallelMERuntime() {
		System.loadLibrary("ParallelMEGenerated");
		this.runtimePointer = nativeInit();
	}

	@Override
	protected void finalize() throws Throwable {
		nativeCleanUpRuntime(runtimePointer);
		super.finalize();
	}

	public void cleanUpArray(long arrayPointer) {
		nativeCleanUpArray(arrayPointer);
	}

	public long createArray(short[] array) {
		return nativeCreateShortArray(runtimePointer, array, array.length);
	}

	public long createArray(int[] array) {
		return nativeCreateIntArray(runtimePointer, array, array.length);
	}

	public long createArray(float[] array) {
		return nativeCreateFloatArray(runtimePointer, array, array.length);
	}

	public long toArray(long arrayPointer, short[] array) {
		return nativeToArray(runtimePointer, arrayPointer, array);
	}

	public long toArray(long arrayPointer, int[] array) {
		return nativeToArray(runtimePointer, arrayPointer, array);
	}

	public long toArray(long arrayPointer, float[] array) {
		return nativeToArray(runtimePointer, arrayPointer, array);
	}
	
	public long createBitmapImage(Bitmap bitmap) {
		return nativeCreateBitmapImage(runtimePointer, bitmap, bitmap.getWidth(), bitmap.getHeight());
	}

	public void toBitmapBitmapImage(long imagePointer, Bitmap bitmap) {
		nativeToBitmapBitmapImage(runtimePointer, imagePointer, bitmap);
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

	public int getLength(long arrayPointer) {
		return nativeGetLength(arrayPointer);
	}}