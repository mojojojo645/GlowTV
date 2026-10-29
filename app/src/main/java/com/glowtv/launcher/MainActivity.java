package com.glowtv.launcher;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    // Row order. Edit this list to change your favourites.
    static final String[][] APPS = {
        {"YouTube", "com.google.android.youtube.tv"},
        {"Netflix", "com.netflix.ninja"},
        {"Viaplay", "com.viaplay.android"},
        {"HBO Max", "com.wbd.stream"},
        {"Stremio", "com.stremio.one"},
        {"7Reels", "cc.sevenreels.app"},
        {"RAD TV", "com.shadeed.Radtvplay"},
        {"Crunchyroll", "com.crunchyroll.crunchyroid"},
        {"Settings", null},
    };
    static final TimeInterpolator EASE = new PathInterpolator(0.2f, 0f, 0f, 1f);

    final Handler h = new Handler(Looper.getMainLooper());
    TextView clock, date, greet, hero;
    LinearLayout row;
    HorizontalScrollView scroll;
    AuroraView aurora;
    ObjectAnimator scrollAnim;
    int W, tileW, tileH;

    final Runnable tick = new Runnable() {
        public void run() {
            Date d = new Date();
            Locale l = Locale.getDefault();
            clock.setText(DateFormat.getTimeFormat(MainActivity.this).format(d));
            date.setText(new SimpleDateFormat("EEEE d MMMM", l).format(d).toUpperCase(l));
            int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            greet.setText(hr < 5 ? "Good night" : hr < 12 ? "Good morning" : hr < 18 ? "Good afternoon" : "Good evening");
            h.postDelayed(this, 10000);
        }
    };

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    // Drop Inter font files named inter_light.ttf and inter_medium.ttf into app/src/main/res/font/ to use them.
    Typeface tf(String res, String fallback) {
        try {
            int id = getResources().getIdentifier(res, "font", getPackageName());
            if (id != 0) return getResources().getFont(id);
        } catch (Exception e) { }
        return Typeface.create(fallback, Typeface.NORMAL);
    }

    TextView text(float sp, int color, String res, String fb, float spacing) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf(res, fb));
        t.setLetterSpacing(spacing);
        t.setIncludeFontPadding(false);
        return t;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        W = getResources().getDisplayMetrics().widthPixels;
        int H = getResources().getDisplayMetrics().heightPixels;
        tileW = (int) (W * 0.165f);
        tileH = tileW * 9 / 16;
        int gap = (int) (W * 0.017f);

        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);
        aurora = new AuroraView(this);
        root.addView(aurora);

        // Header: greeting left, clock + date right
        greet = text(26, 0xCCFFFFFF, "inter_medium", "sans-serif-medium", 0.01f);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        gp.setMargins(dp(56), dp(48), 0, 0);
        root.addView(greet, gp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setGravity(Gravity.END);
        clock = text(54, 0xFFFFFFFF, "inter_light", "sans-serif-light", 0.01f);
        clock.setShadowLayer(dp(14), 0, 0, 0x66FFFFFF);
        date = text(13, 0x99FFFFFF, "inter_medium", "sans-serif-medium", 0.14f);
        top.addView(clock);
        top.addView(date);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        tp.setMargins(0, dp(36), dp(56), 0);
        root.addView(top, tp);

        // Row of landscape tiles, scrolled so the focused tile is always centred
        int rowBottom = (int) (H * 0.08f);
        int vpad = dp(48);
        scroll = new HorizontalScrollView(this) {
            @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect r) { return 0; }
        };
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        int side = (W - tileW) / 2;
        row.setPadding(side, vpad, side, vpad);
        for (int i = 0; i < APPS.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
            lp.setMarginEnd(i == APPS.length - 1 ? 0 : gap);
            row.addView(new Item(APPS[i][0], APPS[i][1], i), lp);
        }
        scroll.addView(row, new FrameLayout.LayoutParams(-2, -2));
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        sp.bottomMargin = rowBottom;
        root.addView(scroll, sp);

        // Big title of the focused app
        hero = text(54, 0xFFFFFFFF, "inter_light", "sans-serif-light", -0.01f);
        hero.setShadowLayer(dp(16), 0, 0, 0x55FFFFFF);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        hp.setMargins(dp(56), 0, 0, rowBottom + vpad + tileH + dp(44));
        root.addView(hero, hp);

        setContentView(root);

        // Staggered entrance
        for (int i = 0; i < row.getChildCount(); i++) {
            View v = row.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(40));
            v.animate().alpha(0.78f).translationY(0).setStartDelay(150 + i * 60L).setDuration(600).setInterpolator(EASE).start();
        }
        greet.setAlpha(0f);
        greet.animate().alpha(1f).setStartDelay(300).setDuration(800).start();
        top.setAlpha(0f);
        top.animate().alpha(1f).setStartDelay(200).setDuration(800).start();
        row.getChildAt(0).requestFocus();
    }

    void setHero(String s) {
        if (s.contentEquals(hero.getText())) return;
        hero.animate().cancel();
        hero.animate().alpha(0f).translationX(-dp(12)).setDuration(110).withEndAction(() -> {
            hero.setText(s);
            hero.setTranslationX(dp(18));
            hero.animate().alpha(1f).translationX(0).setDuration(320).setInterpolator(EASE).start();
        }).start();
    }

    void onItemFocus(Item it) {
        setHero(it.name);
        aurora.setFocus(it.idx / (float) (APPS.length - 1));
        int target = Math.max(0, it.getLeft() + it.getWidth() / 2 - W / 2);
        if (scrollAnim != null) scrollAnim.cancel();
        scrollAnim = ObjectAnimator.ofInt(scroll, View.SCROLL_X, scroll.getScrollX(), target);
        scrollAnim.setDuration(420);
        scrollAnim.setInterpolator(EASE);
        scrollAnim.start();
    }

    class Item extends FrameLayout {
        final String name, pkg;
        final int idx;
        final View shine, rim;

        Item(String n, String p, int i) {
            super(MainActivity.this);
            name = n;
            pkg = p;
            idx = i;
            float rad = tileH * 0.14f;
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF2C2C2E, 0xFF1C1C1E});
            bg.setCornerRadius(rad);
            setBackground(bg);
            setClipToOutline(true);
            setFocusable(true);
            setClickable(true);
            setAlpha(0.78f);
            if (Build.VERSION.SDK_INT >= 28) {
                setOutlineAmbientShadowColor(Color.WHITE);
                setOutlineSpotShadowColor(Color.WHITE);
            }
            addView(content(p));

            shine = new View(getContext());
            shine.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0x45FFFFFF, 0x00FFFFFF}));
            shine.setAlpha(0f);
            addView(shine);

            rim = new View(getContext());
            GradientDrawable rd = new GradientDrawable();
            rd.setCornerRadius(rad);
            rd.setStroke(dp(2), 0xCCFFFFFF);
            rim.setBackground(rd);
            rim.setAlpha(0f);
            addView(rim);

            setOnClickListener(v -> launch(pkg, this));
        }

        View content(String p) {
            if (p == null) return new GearView(getContext());
            PackageManager pm = getPackageManager();
            Drawable b = null;
            try {
                Intent li = pm.getLeanbackLaunchIntentForPackage(p);
                if (li != null) b = pm.getActivityBanner(li);
            } catch (Exception e) { }
            if (b == null) { try { b = pm.getApplicationBanner(p); } catch (Exception e) { } }
            ImageView iv = new ImageView(getContext());
            if (b != null) {
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageDrawable(b);
                return iv;
            }
            try {
                iv.setImageDrawable(pm.getApplicationIcon(p));
                int pad = (int) (tileH * 0.2f);
                iv.setPadding(pad, pad, pad, pad);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                return iv;
            } catch (Exception e) {
                TextView t = text(34, 0xFFFFFFFF, "inter_medium", "sans-serif-medium", 0f);
                t.setGravity(Gravity.CENTER);
                t.setText(name.substring(0, 1));
                return t;
            }
        }

        @Override protected void onFocusChanged(boolean g, int d, Rect r) {
            super.onFocusChanged(g, d, r);
            animate().cancel();
            animate().setStartDelay(0).scaleX(g ? 1.1f : 1f).scaleY(g ? 1.1f : 1f)
                .translationZ(g ? dp(28) : 0).translationY(0).alpha(g ? 1f : 0.78f)
                .setDuration(320).setInterpolator(EASE).start();
            shine.animate().alpha(g ? 1f : 0f).setDuration(320).start();
            rim.animate().alpha(g ? 1f : 0f).setDuration(320).start();
            if (g) onItemFocus(this);
        }
    }

    /** Thin-line gear icon for the Settings tile. */
    static class GearView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        GearView(Context c) {
            super(c);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.WHITE);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        @Override protected void onDraw(Canvas c) {
            float s = getHeight(), cx = getWidth() / 2f, cy = getHeight() / 2f;
            p.setStrokeWidth(s * 0.035f);
            c.drawCircle(cx, cy, s * 0.17f, p);
            c.drawCircle(cx, cy, s * 0.07f, p);
            p.setStrokeWidth(s * 0.06f);
            for (int i = 0; i < 8; i++) {
                c.save();
                c.rotate(i * 45, cx, cy);
                c.drawLine(cx, cy - s * 0.21f, cx, cy - s * 0.27f, p);
                c.restore();
            }
        }
    }

    void launch(String pkg, View from) {
        try {
            Intent i;
            if (pkg == null) i = new Intent(Settings.ACTION_SETTINGS);
            else {
                i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
            }
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i, ActivityOptions.makeScaleUpAnimation(from, 0, 0, from.getWidth(), from.getHeight()).toBundle());
            } else Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not open", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onResume() { super.onResume(); h.removeCallbacks(tick); h.post(tick); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(tick); }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); row.getChildAt(0).requestFocus(); }
    @Override public void onBackPressed() { /* a launcher never exits */ }

    @Override public void onWindowFocusChanged(boolean f) {
        super.onWindowFocusChanged(f);
        if (f) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
