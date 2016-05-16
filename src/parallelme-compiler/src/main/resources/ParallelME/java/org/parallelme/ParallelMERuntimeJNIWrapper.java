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
 * @author Wilson de Carvalho, Pedro Caldeira
 */
public final class ParallelMERuntimeJNIWrapper implements ParallelMERuntimeDefinition {
	private static final ParallelMERuntimeJNIWrapper instance = new ParallelMERuntimeJNIWrapper();
	public final long runtimePointer = init();

	public static ParallelMERuntimeJNIWrapper getInstance() {
		return this.instance;
	}

	private native long init();
	private native void cleanUp(long runtimePointer);
    private native void createHDRImage(long runtimePointer, byte[] data, int width, int height);
    private native void toBitmap(long runtimePointer, Bitmap bitmap);
    private native int getHeight(long runtimePointer);
    private native int getWidth(long runtimePointer);
	
	@Override
	protected void finalize throws Throwable {
		cleanUp(runtimePointer);
		super.finalize();
	}

	static {
		System.loadLibrary("ParallelME");
	}

	public void createHDRImage(RGBE.ResourceData imageResourceData) {
		createHDRImage(runtimePointer, imageResourceData.data, imageResourceData.width, imageResourceData.height);
	}

	public void toBitmap(Bitmap bitmap) {
		toBitmap(runtimePointer, bitmap);
	}

	public int getHeight() {
		getHeight(runtimePointer);
	}

	public int getWidth() {
		getWidth(runtimePointer);
	}
}
