package com.fls.imageprocessor;

import android.graphics.Bitmap;

public class LogicResult {
    public long result;
    public double probability;
    public Bitmap bitmap;

    public LogicResult(long result, double probability, Bitmap bitmap) {
        this.result = result;
        this.probability = probability;
        this.bitmap = bitmap;
    }
}

