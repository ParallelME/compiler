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
 * responsible for keeping the runtime pointer in memory so it
 * can be used in multiple calls to the low-level runtime without
 * recreating objects.
 *
 * @author Pedro Caldeira, Wilson de Carvalho
 */
public final class ParallelMERuntime {
	private static final ParallelMERuntime instance = new ParallelMERuntime();
	public final long runtimePointer = init();

	public static ParallelMERuntime getInstance() {
		return this.instance;
	}

	private native long init();
	private native void cleanUp(long runtimePointer);
    private native long createHDRImage(long runtimePointer, byte[] data, int width, int height);
    private native void toBitmapHDRImage(long runtimePointer, long imagePointer, Bitmap bitmap);
    private native int getHeight(long imagePointer);
    private native int getWidth(long imagePointer);
	
	@Override
	protected void finalize throws Throwable {
		cleanUp(runtimePointer);
		super.finalize();
	}

	static {
		System.loadLibrary("ParallelME");
	}

	public int createHDRImage(byte[] data, int width, int heigth) {
		return createHDRImage(runtimePointer, data, width, height);
	}

	public void toBitmapHDRImage(long imagePointer, Bitmap bitmap) {
		toBitmapHDRImage(runtimePointer, imagePointer, bitmap);
	}

	public int getHeight(long imagePointer) {
		return getHeight(imagePointer);
	}

	public int getWidth(long imagePointer) {
		return getWidth(imagePointer);
	}
}
