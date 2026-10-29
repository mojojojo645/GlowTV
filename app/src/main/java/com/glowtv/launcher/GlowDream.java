package com.glowtv.launcher;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.os.*;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import java.util.Date;

/** Screensaver: a big glowing clock that drifts and breathes over the aurora background. */
public class GlowDream extends DreamService {
    final Handler h = new Handler(Looper.getMainLooper());
    FrameLayout root;
    TextView clock;
    ValueAnimator drift;

    final Runnable tick = new Runnable() {
        public void run() {
            clock.setText(DateFormat.getTimeFormat(GlowDream.this).format(new Date()));
            h.postDelayed(this, 10000);
        }
    };

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        root = new FrameLayout(this);
        root.addView(new AuroraView(this));
        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(110);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        clock.setShadowLayer(40, 0, 0, Color.WHITE);
        root.addView(clock, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        setContentView(root);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        h.post(tick);
        drift = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        drift.setDuration(120000);
        drift.setRepeatCount(ValueAnimator.INFINITE);
        drift.setInterpolator(new LinearInterpolator());
        drift.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            clock.setTranslationX((float) Math.sin(v) * root.getWidth() * 0.18f);
            clock.setTranslationY((float) Math.cos(v * 2) * root.getHeight() * 0.2f);
            clock.setAlpha(0.75f + 0.25f * (float) Math.sin(v * 6));
        });
        drift.start();
    }

    @Override public void onDreamingStopped() {
        h.removeCallbacks(tick);
        if (drift != null) drift.cancel();
        super.onDreamingStopped();
    }
}
