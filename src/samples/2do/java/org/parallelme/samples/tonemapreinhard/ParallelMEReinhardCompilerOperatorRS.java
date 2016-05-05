/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.samples.tonemapreinhard;

import android.graphics.Bitmap;
import android.support.v8.renderscript.*;
import org.parallelme.userlibrary.image.RGBE;

public class ParallelMEReinhardCompilerOperatorRS implements ParallelMEReinhardCompilerOperator {
    private RenderScript $mRS;
    private ScriptC_ReinhardCompilerOperator $kernel_ReinhardCompilerOperator;
    private Allocation $imageIn, $imageOut;

    public ParallelMEReinhardCompilerOperatorRS(RenderScript $rs) {
        $mRS = $rs;
        $kernel_ReinhardCompilerOperator = new ScriptC_ReinhardCompilerOperator($mRS);
    }

    public boolean valid() {
        return true;
    }

    public void createHDRImage(RGBE.ResourceData $imageResourceData) {
        Type $imageInDataType = new Type.Builder($mRS, Element.RGBA_8888($mRS))
                .setX($imageResourceData.width)
                .setY($imageResourceData.height)
                .create();
        Type $imageOutDataType = new Type.Builder($mRS, Element.F32_4($mRS))
                .setX($imageResourceData.width)
                .setY($imageResourceData.height)
                .create();
        $imageIn = Allocation.createTyped($mRS, $imageInDataType);
        $imageOut = Allocation.createTyped($mRS, $imageOutDataType);
        $imageIn.copyFrom($imageResourceData.data);
        $kernel_ReinhardCompilerOperator.forEach_toFloat($imageIn, $imageOut);
    }

    public void toBitmap(Bitmap bitmap) {
        $kernel_ReinhardCompilerOperator.forEach_toBitmap($imageOut, $imageIn);
        $imageIn.copyTo(bitmap);
        $imageIn = null;
        $imageOut = null;
    }

    public int getHeight() {
        return $imageIn.getType().getY();
    }

    public int getWidth() {
        return $imageOut.getType().getX();
    }

    public void iterator1() {
        $kernel_ReinhardCompilerOperator.forEach_iterator1($imageOut, $imageOut);
    }

    public void iterator2(float sum, float[] $sumIterator2, float max, float[] $maxIterator2) {
        Allocation $gSumIterator2_Allocation = Allocation.createSized($mRS, Element.F32($mRS), 1);
        $kernel_ReinhardCompilerOperator.set_gSumIterator2(sum);
        $kernel_ReinhardCompilerOperator.set_gOutputSumIterator2($gSumIterator2_Allocation);

        Allocation $gMaxIterator2_Allocation = Allocation.createSized($mRS, Element.F32($mRS), 1);
        $kernel_ReinhardCompilerOperator.set_gMaxIterator2(max);
        $kernel_ReinhardCompilerOperator.set_gOutputMaxIterator2($gMaxIterator2_Allocation);

        $kernel_ReinhardCompilerOperator.set_gInputImageIterator2($imageOut);
        $kernel_ReinhardCompilerOperator.set_gInputXSizeIterator2($imageOut.getType().getX());
        $kernel_ReinhardCompilerOperator.set_gInputYSizeIterator2($imageOut.getType().getY());
        $kernel_ReinhardCompilerOperator.invoke_iterator2();

        $gSumIterator2_Allocation.copyTo($sumIterator2);
        $gMaxIterator2_Allocation.copyTo($maxIterator2);
    }

    public void iterator3(final float scaleFactor, final float lmax2) {
        $kernel_ReinhardCompilerOperator.set_gScaleFactorIterator3(scaleFactor);
        $kernel_ReinhardCompilerOperator.set_gLmax2Iterator3(lmax2);
        $kernel_ReinhardCompilerOperator.forEach_iterator3($imageOut, $imageOut);
    }

    public void iterator4() {
        $kernel_ReinhardCompilerOperator.forEach_iterator4($imageOut, $imageOut);
    }

    public void iterator5(final float power) {
        $kernel_ReinhardCompilerOperator.set_gPowerIterator5(power);
        $kernel_ReinhardCompilerOperator.forEach_iterator5($imageOut, $imageOut);
    }
}
