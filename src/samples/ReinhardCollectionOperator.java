/**                                               _    __ ____
 *   _ __  ___ _____   ___   __  __   ___ __     / |  / /  __/
 *  |  _ \/ _ |  _  | / _ | / / / /  / __/ /    /  | / / /__
 *  |  __/ __ |  ___|/ __ |/ /_/ /__/ __/ /__  / / v  / /__
 *  |_| /_/ |_|_|\_\/_/ |_/____/___/___/____/ /_/  /_/____/
 *
 */

package org.parallelme.samples.tonemapreinhard;

import android.graphics.Bitmap;

import org.parallelme.userlibrary.function.ForeachFunction;
import org.parallelme.userlibrary.image.HDRImage;
import org.parallelme.userlibrary.image.Pixel;
import org.parallelme.userlibrary.image.RGBE;

/**
 * Implementation of the Tonemap Reinhard algorithm using the ParallelME user library.
 *
 * @author Pedro Caldeira, Renato Utsch
 */
public class ReinhardCollectionOperator implements ReinhardOperator {
    private HDRImage image;
    private float sum;
    private float max;
    private float scaleFactor;
    private float lmax2;

    public void runOp(RGBE.ResourceData resourceData, float key, float power, Bitmap bitmap) {
        image = new HDRImage(resourceData.data, resourceData.width, resourceData.height);

        this.toYxy();
        this.logAverage(key);
        this.tonemap();
        this.toRgb();
        this.clamp(power);
        image.toBitmap(bitmap);
    }

    public void waitFinish() {
        // Do nothing.
    }

    private void toYxy(){
        image.par().foreach(new Foreach<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                float result0, result1, result2;
                float w;

                result0 = result1 = result2 = 0.0f;
                result0 += 0.5141364f * pixel.rgba.red;
                result0 += 0.3238786f * pixel.rgba.green;
                result0 += 0.16036376f * pixel.rgba.blue;
                result1 += 0.265068f * pixel.rgba.red;
                result1 += 0.67023428f * pixel.rgba.green;
                result1 += 0.06409157f * pixel.rgba.blue;
                result2 += 0.0241188f * pixel.rgba.red;
                result2 += 0.1228178f * pixel.rgba.green;
                result2 += 0.84442666f * pixel.rgba.blue;
                w = result0 + result1 + result2;
                if (w > 0.0) {
                    pixel.rgba.red = result1;
                    pixel.rgba.green = result0 / w;
                    pixel.rgba.blue = result1 / w;
                } else {
                    pixel.rgba.red = pixel.rgba.green = pixel.rgba.blue = 0.0f;
                }
            }
        });
    }


    //This is a good example. We lack a way to return single/multiple values from all kernel instances.
    private void logAverage(float key) {
        sum = 0.0f;
        max = 0.0f;

        image.par().foreach(new Foreach<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                sum += Math.log(0.00001f + pixel.rgba.red);

                if(pixel.rgba.red > max)
                    max = pixel.rgba.red;
            }
        });

        // Calculate the scale factor.
        float average = (float) Math.exp(sum / (float)(image.getHeight() * image.getWidth()));
        scaleFactor = key * (1.0f / average);

        // lmax2.
        lmax2 = (float) Math.pow(max * scaleFactor, 2);
    }

    private void tonemap() {
        final float fScaleFactor = scaleFactor;
        final float fLmax2 = lmax2;
        image.par().foreach(new Foreach<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                // Scale to midtone.
                pixel.rgba.red *= fScaleFactor;

                // Tonemap.
                pixel.rgba.red *= (1.0f + pixel.rgba.red / fLmax2) / (1.0f + pixel.rgba.red);
            }
        });
    }

    private void toRgb(){
        image.par().foreach(new Foreach<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                float _x, _y, _z, g, b;

                _y = pixel.rgba.red;    // Y
                g = pixel.rgba.green;   // x
                b = pixel.rgba.blue;    // y

                if (_y > 0.0f && g > 0.0f && b > 0.0f) {
                    _x = g * _y / b;
                    _z = _x / g - _x - _y;
                } else {
                    _x = _z = 0.0f;
                }

                // These constants are the conversion coefficients.
                pixel.rgba.red = pixel.rgba.green = pixel.rgba.blue = 0.0f;
                pixel.rgba.red += 2.5651f * _x;
                pixel.rgba.red += -1.1665f * _y;
                pixel.rgba.red += -0.3986f * _z;
                pixel.rgba.green += -1.0217f * _x;
                pixel.rgba.green += 1.9777f * _y;
                pixel.rgba.green += 0.0439f * _z;
                pixel.rgba.blue += 0.0753f * _x;
                pixel.rgba.blue += -0.2543f * _y;
                pixel.rgba.blue += 1.1892f * _z;
            }
        });
    }

    private void clamp(final float power) {
        image.par().foreach(new Foreach<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                // Clamp.
                if (pixel.rgba.red > 1.0f) pixel.rgba.red = 1.0f;
                if (pixel.rgba.green > 1.0f) pixel.rgba.green = 1.0f;
                if (pixel.rgba.blue > 1.0f) pixel.rgba.blue = 1.0f;

                pixel.rgba.red = (float) Math.pow(pixel.rgba.red, power);
                pixel.rgba.green = (float) Math.pow(pixel.rgba.green, power);
                pixel.rgba.blue = (float) Math.pow(pixel.rgba.blue, power);
            }
        });
    }
}
