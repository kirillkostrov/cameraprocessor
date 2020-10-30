package com.fls.imageprocessor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class Logic {
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void Run(Bitmap bitmap, int x, int y, int z, LogicResultCallback resultCallback) {

        long width = bitmap.getWidth();
        long height = bitmap.getHeight();
        Color pixelColor = bitmap.getColor(Math.round(width / 2), Math.round(height / 2));

        /* DO SOMETHING */

        LogicResult result = new LogicResult(Color.pack(pixelColor.red(), pixelColor.green(), pixelColor.blue()), 0.5);
        resultCallback.OnResult(result);
    }

}

