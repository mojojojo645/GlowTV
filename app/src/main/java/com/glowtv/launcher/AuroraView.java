package com.glowtv.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Black background with three slow-drifting white glows. No allocations per frame. */
public class AuroraView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator va;
    private float t, r;

    public AuroraView(Context c) { super(c); }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        r = Math.max(w, h) * 0.5f;
        p.setShader(new RadialGradient(0, 0, r, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        va = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        va.setDuration(90000);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(a -> { t = (float) a.getAnimatedValue(); invalidate(); });
        va.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (va != null) va.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        c.drawColor(Color.BLACK);
        int w = getWidth(), h = getHeight();
        for (int i = 0; i < 3; i++) {
            float cx = w * (0.5f + 0.38f * (float) Math.sin(t * (i + 1) + i * 2.1f));
            float cy = h * (0.5f + 0.30f * (float) Math.cos(t * (i + 1) + i * 1.3f));
            p.setAlpha(i == 0 ? 255 : 170 - i * 40);
            c.save();
            c.translate(cx, cy);
            c.drawCircle(0, 0, r, p);
            c.restore();
        }
    }
}
