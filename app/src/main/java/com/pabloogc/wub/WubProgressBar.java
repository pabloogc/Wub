package com.pabloogc.wub;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import static java.lang.Math.*;

public class WubProgressBar extends View {

  //  private static final Interpolator INTERPOLATOR = new BounceInterpolator();
  private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();
  private static final double PI_HALF = PI / 2.0d;
  public static final boolean DEBUG = Boolean.FALSE;


  private Bitmap bitmap;
  private Canvas bitmapCanvas;

  private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint semiCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint debugPaint = new Paint();

  private final RectF rect = new RectF();
  private final Path path = new Path();
  private int r, cx, cy;

  private float lineWidth;
  private float time;
  private int waves;
  private double amplitude;
  private final int samples;
  private double waveLength;
  private int rotationPeriod;
  private int wavePeriod;
  private float growth;

  private float[] distances;

  public WubProgressBar(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public WubProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setLayerType(LAYER_TYPE_HARDWARE, null);


    TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WubProgressBar, 0, 0);
    waves = ta.getInt(R.styleable.WubProgressBar_waves, 8);
    waveLength = toRadians(ta.getFloat(R.styleable.WubProgressBar_waveLength, 360.0f / waves));
    amplitude = waves * waveLength;
    samples = ta.getInt(R.styleable.WubProgressBar_samples, 7);
    growth = ta.getDimension(R.styleable.WubProgressBar_growth, 15);
    lineWidth = ta.getDimension(R.styleable.WubProgressBar_lineWidth, 8);
    rotationPeriod = ta.getInt(R.styleable.WubProgressBar_rotationPeriod, 3500);
    wavePeriod = ta.getInt(R.styleable.WubProgressBar_wavePeriod, 500);
    ta.recycle();

    debugPaint.setColor(Color.BLUE);

    paint.setStrokeWidth(lineWidth);
    paint.setDither(true);
    paint.setStrokeJoin(Paint.Join.ROUND);
    paint.setStyle(Paint.Style.STROKE);
    if (!isInEditMode()) {
      paint.setPathEffect(new CornerPathEffect(10));
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
    }

    semiCirclePaint.setStyle(Paint.Style.STROKE);
    semiCirclePaint.setColor(Color.WHITE);
    semiCirclePaint.setStrokeWidth(lineWidth);
    semiCirclePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        PropertyValuesHolder time = PropertyValuesHolder.ofFloat("time", 0, (float) (2 * Math.PI));
    ObjectAnimator timeAnim = ObjectAnimator.ofPropertyValuesHolder(this, time);
    timeAnim.setDuration(wavePeriod);
    timeAnim.setInterpolator(new LinearInterpolator());
    timeAnim.setRepeatCount(ValueAnimator.INFINITE);
    timeAnim.setRepeatMode(ValueAnimator.RESTART);
    if (wavePeriod > 0) timeAnim.start();

    PropertyValuesHolder rotation = PropertyValuesHolder.ofFloat("rotation", 0, 360);
    ObjectAnimator rotateAnim = ObjectAnimator.ofPropertyValuesHolder(this, rotation);
    rotateAnim.setDuration(rotationPeriod);
    rotateAnim.setInterpolator(new LinearInterpolator());
    rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
    rotateAnim.setRepeatMode(ValueAnimator.RESTART);
    if (rotationPeriod > 0) rotateAnim.start();
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (!changed) return;

    cx = getWidth() / 2;
    cy = getHeight() / 2;
    r = (int) (min(cx, cy) - growth);

    if (bitmap != null) {
      bitmap.recycle();
      bitmap = null;
    }
  }

  @Override protected void onDraw(Canvas canvas) {
    if (bitmap == null) {
      bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
      bitmapCanvas = new Canvas(bitmap);
    }

    bitmapCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

    paint.setColor(Color.RED);
    drawVibration(bitmapCanvas, 0, paint);

    paint.setColor(Color.GREEN);
    drawVibration(bitmapCanvas, 2 * PI / 3, paint);

    paint.setColor(Color.BLUE);
    drawVibration(bitmapCanvas, PI, paint);

    paint.setColorFilter(null);

    final int angle = (int) (toDegrees(amplitude));
    rect.set(cx - r, cy - r, cx + r, cy + r);
    semiCirclePaint.setColor(Color.WHITE);
    bitmapCanvas.drawArc(rect, angle, 360 - angle, false, semiCirclePaint);

    canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
  }

  private void drawVibration(Canvas canvas, double timeOffset, Paint paint) {
    path.reset();
    path.moveTo(cx + r, cy);

    if (DEBUG) {
      debugPaint.setColor(Color.BLUE);
      canvas.drawLine(cx - r, cy, cx + r, cy, debugPaint);
      canvas.drawLine(cx, cy + r, cx, cy - r, debugPaint);

      debugPaint.setColor(Color.RED);
      canvas.drawCircle(cx + r, cy, 10, debugPaint);
    }

    final double step = 2 * PI / samples;
    final double waveStep = waveLength / samples;

    for (int w = 0; w < waves; w++) {
      final long s = w == waves - 1 ? samples + 2 : samples;
      for (int i = 0; i < s + 2; i++) {
        final double distance = i * waveStep + w * waveLength;
        final double fraction = growFunction((amplitude - distance) / amplitude);
        final double factor = 1 - Math.abs(fraction * -2 + 1);

        double rad = r + growth * factor * factor * sin(i * step + time + timeOffset);
        float x = (float) (rad * cos(distance));
        float y = (float) (rad * sin(distance));
        path.lineTo(cx + x, cy + y);
      }
    }
    canvas.drawPath(path, paint);
  }

  private double growFunction(final double input) {
    return INTERPOLATOR.getInterpolation((float) input);
  }

  private void setTime(float time) {
    this.time = time;
    invalidate();
  }
}
