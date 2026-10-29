package com.glowtv.launcher;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.os.*;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Screensaver: a large glowing clock that drifts and breathes over the aurora background. */
public class GlowDream extends DreamService {
    final Handler h = new Handler(Looper.getMainLooper());
    FrameLayout root;
    LinearLayout box;
    TextView clock, date;
    ValueAnimator drift;

    final Runnable tick = new Runnable() {
        public void run() {
            Date d = new Date();
            Locale l = Locale.getDefault();
            clock.setText(DateFormat.getTimeFormat(GlowDream.this).format(d));
            date.setText(new SimpleDateFormat("EEEE d MMMM", l).format(d).toUpperCase(l));
            h.postDelayed(this, 10000);
        }
    };

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        root = new FrameLayout(this);
        root.addView(new AuroraView(this));
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(120);
        clock.setIncludeFontPadding(false);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setShadowLayer(40, 0, 0, 0xCCFFFFFF);
        date = new TextView(this);
        date.setTextColor(0x99FFFFFF);
        date.setTextSize(18);
        date.setLetterSpacing(0.16f);
        date.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(clock);
        box.addView(date);
        root.addView(box, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
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
            box.setTranslationX((float) Math.sin(v) * root.getWidth() * 0.16f);
            box.setTranslationY((float) Math.cos(v * 2) * root.getHeight() * 0.18f);
            box.setAlpha(0.78f + 0.22f * (float) Math.sin(v * 6));
        });
        drift.start();
    }

    @Override public void onDreamingStopped() {
        h.removeCallbacks(tick);
        if (drift != null) drift.cancel();
        super.onDreamingStopped();
    }
}
