package org.parallelme.samples.tonemapreinhard;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.parallelme.userlibrary.function.UserFunction;
import org.parallelme.userlibrary.image.HDRImage;
import org.parallelme.userlibrary.image.Pixel;
import org.parallelme.samples.tonemapreinhard.formats.RGB;
import org.parallelme.userlibrary.image.Image;

/**
 * @author Pedro Caldeira
 */
public class ReinhardCollectionOperator implements ReinhardOperator {
    private HDRImage image;
    private float sum = 0.0f;
    private float max = 0.0f;

    @Override
    public Bitmap runOp(Resources res, int resource, float key, float gamma) {
        image = new HDRImage(res, resource);
		
        this.toYxy();
        this.scaleToMidtone(key);
        this.tonemap();
        this.toRgb();
        this.power(gamma);
		Bitmap bitmap = image.toBitmap();
        return bitmap;
    }


    private void toYxy(){
        image.par().foreach(new ForeachFunction<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                float result_0, result_1, result_2;
                float w;
                result_0 = result_1 = result_2 = 0.0f;
                result_0 += 0.5141364f * pixel.rgba.red;
                result_0 += 0.3238786f * pixel.rgba.green;
                result_0 += 0.16036376f * pixel.rgba.blue;
                result_1 += 0.265068f * pixel.rgba.red;
                result_1 += 0.67023428f * pixel.rgba.green;
                result_1 += 0.06409157f * pixel.rgba.blue;
                result_2 += 0.0241188f * pixel.rgba.red;
                result_2 += 0.1228178f * pixel.rgba.green;
                result_2 += 0.84442666f * pixel.rgba.blue;
                w = result_0 + result_1 + result_2;
                if (w > 0) {
                    pixel.rgba.red = result_1;
                    pixel.rgba.green = result_0 / w;
                    pixel.rgba.blue = result_1 / w;
                } else {
                    pixel.rgba.red = pixel.rgba.green = pixel.rgba.blue = 0.0f;
                }
            }
        });
    }


    //This is a good example. We lack a way to return a single value from all kernel instances.
    private float logAverage() {
        sum = 0;

        image.par().foreach(new ForeachFunction<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                sum += Math.log(0.00001f + pixel.rgba.red);
            }
        });

        return (float)Math.exp(sum/(image.getHeight()*image.getWidth()));
    }

    private void scaleToMidtone(final float key) {
        final float scaleFactor = 1.0f / this.logAverage();

        image.par().foreach(new ForeachFunction<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                pixel.rgba.red *= scaleFactor * key;
            }
        });
    }

    private float getMaxValue() {
        max = 0;
        image.par().foreach(new ForeachFunction<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                if (pixel.rgba.red > max) max = pixel.rgba.red;
            }
        });

        return max;
    }

    private void tonemap() {
        final float max2 = (float)Math.pow(getMaxValue(), 2);
        image.par().foreach(new ForeachFunction<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                pixel.rgba.red *= (1.0f + pixel.rgba.red / max2) / (1.0f + pixel.rgba.red);
            }
        });
    }

    private void toRgb(){
        image.par().foreach(new ForeachFunction<Pixel>() {
            @Override
            public void function(Pixel pixel) {
				float val_r, val_g, val_b;
				float result_r, result_g, result_b;
				float out_r, out_g, out_b;
                val_g = pixel.rgba.red;     // Y
                result_g = pixel.rgba.green; // x
                result_b = pixel.rgba.blue; // y
                if (val_g > 0.0f && result_g > 0.0f && result_b > 0.0f) {
                    val_r = result_g * val_g / result_b;
                    val_b = val_r / result_g - val_r - val_g;
                } else {
                    val_r = val_b = 0.0f;
                }
                // These constants are the conversion coefficients.
                out_r = out_g = out_b = 0.0f;
                out_r += 2.5651f * val_r;
                out_r += -1.1665f * val_g;
                out_r += -0.3986f * val_b;
                out_g += -1.0217f * val_r;
                out_g += 1.9777f * val_g;
                out_g += 0.0439f * val_b;
                out_b += 0.0753f * val_r;
                out_b += -0.2543f * val_g;
                out_b += 1.1892f * val_b;
                pixel.rgba.red = out_r;
                pixel.rgba.green = out_g;
                pixel.rgba.blue = out_b;
            }
        });
    }

    private void power(final float gamma) {
        final float power = 1.0f / gamma;
        image.par().foreach(new ForeachFunction<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                // Clamp.
                if (pixel.rgba.red > 1.0f) pixel.rgba.red = 1.0f;
                if (pixel.rgba.red < 0.0f) pixel.rgba.red = 0.0f;
                if (pixel.rgba.green > 1.0f) pixel.rgba.green = 1.0f;
                if (pixel.rgba.green < 0.0f) pixel.rgba.green = 0.0f;
                if (pixel.rgba.blue > 1.0f) pixel.rgba.blue = 1.0f;
                if (pixel.rgba.blue < 0.0f) pixel.rgba.blue = 0.0f;
                pixel.rgba.red = (float) Math.pow(pixel.rgba.red, power);
                pixel.rgba.green = (float) Math.pow(pixel.rgba.green, power);
                pixel.rgba.blue = (float) Math.pow(pixel.rgba.blue, power);
                pixel.rgba.alpha = 255;
            }
        });
    }
}