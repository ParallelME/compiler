package org.parallelme.samples.bitmaptest;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v8.renderscript.RenderScript;

import org.parallelme.userlibrary.function.Foreach;
import org.parallelme.userlibrary.image.BitmapImage;
import org.parallelme.userlibrary.image.Pixel;

public class BitmapUserLibraryTest {
    public Bitmap load(Bitmap bitmap) {
        BitmapImage image = new BitmapImage(bitmap);
        // to Yxy
        image.par().foreach(new Foreach<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                float red, green, blue;
                red = green = blue = 0.0f;
                red += 0.5141364f * pixel.rgba.red;
                red += 0.3238786f * pixel.rgba.green;
                red += 0.16036376f * pixel.rgba.blue;
                green += 0.265068f * pixel.rgba.red;
                green += 0.67023428f * pixel.rgba.green;
                green += 0.06409157f * pixel.rgba.blue;
                blue += 0.0241188f * pixel.rgba.red;
                blue += 0.1228178f * pixel.rgba.green;
                blue += 0.84442666f * pixel.rgba.blue;
                float w = red + green + blue;
                if (w > 0.0f) {
                    pixel.rgba.red = green;
                    pixel.rgba.green = red / w;
                    pixel.rgba.blue = green / w;
                } else {
                    pixel.rgba.red = pixel.rgba.green = pixel.rgba.blue = 0.0f;
                }
            }
        });
        // to RGB
        image.par().foreach(new Foreach<Pixel>() {
            @Override
            public void function(Pixel pixel) {
                float xVal, zVal;
                float yVal = pixel.rgba.red;       // Y

                if (yVal > 0.0f && pixel.rgba.green > 0.0f && pixel.rgba.blue > 0.0f) {
                    xVal = pixel.rgba.green * yVal / pixel.rgba.blue;
                    zVal = xVal / pixel.rgba.green - xVal - yVal;
                } else {
                    xVal = zVal = 0.0f;
                }
                pixel.rgba.red = pixel.rgba.green = pixel.rgba.blue = 0.0f;
                pixel.rgba.red += 2.5651f * xVal;
                pixel.rgba.red += -1.1665f * yVal;
                pixel.rgba.red += -0.3986f * zVal;
                pixel.rgba.green += -1.0217f * xVal;
                pixel.rgba.green += 1.9777f * yVal;
                pixel.rgba.green += 0.0439f * zVal;
                pixel.rgba.blue += 0.0753f * xVal;
                pixel.rgba.blue += -0.2543f * yVal;
                pixel.rgba.blue += 1.1892f * zVal;
            }
        });
        bitmap = image.toBitmap();

        return bitmap;
    }
}
