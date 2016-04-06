package com.pabloogc.wub;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;


public class Colors {

    public static final int SEGMENT_COUNT = 10;
    public static int[] rawColors;
    public static int[][] colors;
    public static int[] mainColors;
    public static int[] temperatures;
    private static final float[] hsv = new float[3];

    public static void init(Context context) {
        if (rawColors != null) return;

        rawColors = context.getResources().getIntArray(R.array.colors);
        temperatures = context.getResources().getIntArray(R.array.temps);

        colors = new int[rawColors.length / SEGMENT_COUNT][];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new int[SEGMENT_COUNT];
        }

        for (int i = 0; i < colors.length; i++) {
            System.arraycopy(rawColors, i * SEGMENT_COUNT, colors[i], 0, 10);
        }

        mainColors = new int[rawColors.length / SEGMENT_COUNT];
        for (int i = 0; i < mainColors.length; i++) {
            mainColors[i] = rawColors[i * SEGMENT_COUNT];
        }
    }

    public static int darkenForStatusBar(int color) {
        return brightnessRGB(color, 0.80f);
    }

    public static int brightnessRGB(int color, float factor) {
        int r = (int) Math.min(255, Color.red(color) * factor);
        int g = (int) Math.min(255, Color.green(color) * factor);
        int b = (int) Math.min(255, Color.blue(color) * factor);
        return Color.argb(Color.alpha(color), r, g, b);
    }

    public static int changeAlpha(int color, int alpha) {
        int a = Math.round(alpha);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb(a, r, g, b);
    }


    public static int brightnessHSV(int color, float ammount) {
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.min(1, hsv[2] + ammount);
        return Color.HSVToColor(hsv);
    }

    public static ValueAnimator blend(int color1, int color2, long duration, ValueAnimator.AnimatorUpdateListener listener) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(color1, color2);
        anim.setEvaluator(new ArgbEvaluator());
        anim.addUpdateListener(listener);
        anim.setDuration(duration);
        anim.start();
        return anim;
    }

    public static int contrastBlackOrWhite(int color) {
        return (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114) > 220 ?
                Color.BLACK : Color.WHITE;
    }
}
