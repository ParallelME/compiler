/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.samples.tonemapreinhard;
import android.graphics.Bitmap;
import org.parallelme.userlibrary.image.RGBE;

public interface ParallelMEReinhardCompilerOperator {
    boolean valid();
    void createHDRImage(RGBE.ResourceData $imageResourceData);
    void toBitmap(Bitmap bitmap);
    int getHeight();
    int getWidth();
    void iterator1();
    void iterator2(float sum, float[] outSum, float max, float[] outMax);
    void iterator3(final float scaleFactor, final float lmax2);
    void iterator4();
    void iterator5(final float power);
}
