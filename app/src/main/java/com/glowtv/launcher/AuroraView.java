package com.glowtv.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Black background with soft white light. The main glow follows the focused app. No allocations per frame. */
public class AuroraView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator va;
    private float t, r, focusT = 0.5f, focusC = 0.5f;

    public AuroraView(Context c) { super(c); }

    /** 0..1 horizontal position the main glow should drift towards. */
    public void setFocus(float f) { focusT = f; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        r = Math.max(w, h) * 0.46f;
        p.setShader(new RadialGradient(0, 0, r, 0x34FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        va = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        va.setDuration(120000);
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
        focusC += (focusT - focusC) * 0.045f;
        for (int i = 0; i < 3; i++) {
            float cx, cy;
            if (i == 0) {
                cx = w * (0.2f + 0.6f * focusC) + w * 0.04f * (float) Math.sin(t * 3);
                cy = h * 0.68f + h * 0.04f * (float) Math.cos(t * 2);
                p.setAlpha(255);
            } else {
                cx = w * (0.5f + 0.38f * (float) Math.sin(t * i + i * 2.1f));
                cy = h * (0.3f + 0.2f * (float) Math.cos(t * i + i * 1.3f));
                p.setAlpha(i == 1 ? 120 : 80);
            }
            c.save();
            c.translate(cx, cy);
            c.drawCircle(0, 0, r, p);
            c.restore();
        }
    }
}
