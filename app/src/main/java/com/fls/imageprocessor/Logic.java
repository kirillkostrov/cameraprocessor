package com.fls.imageprocessor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Half;

import androidx.annotation.RequiresApi;

public class Logic {
//    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void Run(Bitmap bitmap, int x, int y, int z, LogicResultCallback resultCallback) {

        long width = bitmap.getWidth();
        long height = bitmap.getHeight();
        long pixel = bitmap.getPixel(Math.round(width / 2), Math.round(height / 2));


        /* DO SOMETHING */
        // Commented to support lower API levels
        //LogicResult result = new LogicResult(Color.pack(pixelColor.red(), pixelColor.green(), pixelColor.blue()), 0.5);

        LogicResult result = new LogicResult(pixel, 0.5, bitmap);
        resultCallback.OnResult(result);
    }
}

