package org.parallelme.samples.tonemapreinhard;

import android.graphics.Bitmap;

import org.parallelme.userlibrary.image.RGBE;

public class ParallelMEReinhardCompilerOperatorCL implements ParallelMEReinhardCompilerOperator {
    private long mPtr; // Stores the native implementation pointer.
    private native long nativeInit();
    private native void nativeCleanUp(long ptr);
    private native void nativeCreateHDRImage(long ptr, byte[] data, int width, int height);
    private native void nativeToBitmap(long ptr, Bitmap bitmap);
    private native int nativeGetHeight(long ptr);
    private native int nativeGetWidth(long ptr);
    private native void nativeIterator1(long ptr);
    private native void nativeIterator2(long ptr, float sum, float[] outSum, float max, float[] outMax);
    private native void nativeIterator3(long ptr, final float scaleFactor, final float lmax2);
    private native void nativeIterator4(long ptr);
    private native void nativeIterator5(long ptr, final float power);

    public ParallelMEReinhardCompilerOperatorCL() {
        mPtr = nativeInit();
    }

    protected void finalize() throws Throwable {
        try {
            nativeCleanUp(mPtr);
            mPtr = 0;
        } catch(Throwable t) {
            throw t;
        } finally {
            super.finalize();
        }
    }

    static {
        System.loadLibrary("TonemapReinhard");
    }

    public boolean valid() {
        return mPtr != 0;
    }

    public void createHDRImage(RGBE.ResourceData imageResourceData) {
        nativeCreateHDRImage(mPtr, imageResourceData.data, imageResourceData.width,
                imageResourceData.height);
    }

    public void toBitmap(Bitmap bitmap) {
        nativeToBitmap(mPtr, bitmap);
    }

    public int getHeight() {
        return nativeGetHeight(mPtr);
    }

    public int getWidth() {
        return nativeGetWidth(mPtr);
    }

    public void iterator1() {
        nativeIterator1(mPtr);
    }

    public void iterator2(float sum, float[] outSum, float max, float[] outMax) {
        nativeIterator2(mPtr, sum, outSum, max, outMax);
    }

    public void iterator3(final float scaleFactor, final float lmax2) {
        nativeIterator3(mPtr, scaleFactor, lmax2);
    }

    public void iterator4() {
        nativeIterator4(mPtr);
    }

    public void iterator5(final float power) {
        nativeIterator5(mPtr, power);
    }
}
