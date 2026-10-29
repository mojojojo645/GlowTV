package com.glowtv.launcher;

import android.app.Activity;
import android.content.Intent;
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
    // Dock order. Edit this list to change your favourites.
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

    final Handler h = new Handler(Looper.getMainLooper());
    TextView clock, date, label;
    LinearLayout dock;

    final Runnable tick = new Runnable() {
        public void run() {
            Date d = new Date();
            clock.setText(DateFormat.getTimeFormat(MainActivity.this).format(d));
            date.setText(new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(d));
            h.postDelayed(this, 10000);
        }
    };

    int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    TextView text(float sp, int color, String face) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(face, Typeface.NORMAL));
        return t;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);
        root.addView(new AuroraView(this));

        // Clock + date, top right, with a soft white glow
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setGravity(Gravity.END);
        clock = text(64, 0xFFFFFFFF, "sans-serif-thin");
        clock.setShadowLayer(dp(18), 0, 0, 0xCCFFFFFF);
        date = text(20, 0x99FFFFFF, "sans-serif-light");
        top.addView(clock);
        top.addView(date);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        tp.setMargins(0, dp(36), dp(52), 0);
        root.addView(top, tp);

        // Dock sizing adapts to the screen width
        int W = getResources().getDisplayMetrics().widthPixels;
        float slot = W * 0.88f / APPS.length;
        int tile = (int) (slot * 0.8f), gap = (int) (slot * 0.1f);
        int dockBottom = dp(44);

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        dock.setPadding(gap * 2, gap * 2, gap * 2, gap * 2);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(tile * 0.5f + gap * 2);
        pill.setColor(0x1FFFFFFF);
        pill.setStroke(dp(1), 0x33FFFFFF);
        dock.setBackground(pill);
        for (String[] a : APPS) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tile, tile);
            lp.setMargins(gap, 0, gap, 0);
            dock.addView(new Tile(a[0], a[1], tile), lp);
        }
        FrameLayout.LayoutParams dp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dp.bottomMargin = dockBottom;
        root.addView(dock, dp);

        // App name above the dock
        label = text(26, 0xFFFFFFFF, "sans-serif-light");
        label.setShadowLayer(dp(12), 0, 0, 0xAAFFFFFF);
        label.setAlpha(0f);
        FrameLayout.LayoutParams ll = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        ll.bottomMargin = dockBottom + tile + gap * 4 + dp(34);
        root.addView(label, ll);

        setContentView(root);

        // Entrance animation
        dock.setAlpha(0f);
        dock.setTranslationY(dp(80));
        dock.animate().alpha(1f).translationY(0).setDuration(700).setInterpolator(new DecelerateInterpolator(2f)).start();
        top.setAlpha(0f);
        top.animate().alpha(1f).setStartDelay(200).setDuration(900).start();
        dock.getChildAt(0).requestFocus();
    }

    class Tile extends FrameLayout {
        final String name, pkg;
        final GradientDrawable bg = new GradientDrawable();

        Tile(String n, String p, int size) {
            super(MainActivity.this);
            name = n;
            pkg = p;
            bg.setCornerRadius(size * 0.24f);
            bg.setColor(0x1AFFFFFF);
            bg.setStroke(dp(1), 0x22FFFFFF);
            setBackground(bg);
            setClipToOutline(true);
            setFocusable(true);
            setClickable(true);
            if (Build.VERSION.SDK_INT >= 28) {
                setOutlineAmbientShadowColor(Color.WHITE);
                setOutlineSpotShadowColor(Color.WHITE);
            }
            Drawable ic = null;
            if (p != null) {
                try { ic = getPackageManager().getApplicationIcon(p); } catch (Exception e) { }
            }
            if (ic != null) {
                ImageView iv = new ImageView(MainActivity.this);
                iv.setImageDrawable(ic);
                int pad = (int) (size * 0.14f);
                iv.setPadding(pad, pad, pad, pad);
                addView(iv);
            } else {
                TextView tv = text(p == null ? 18 : 28, 0xFFFFFFFF, "sans-serif-light");
                tv.setGravity(Gravity.CENTER);
                tv.setText(p == null ? "Settings" : n.substring(0, 1));
                addView(tv, new LayoutParams(-1, -1));
            }
            setOnClickListener(v -> launch(pkg));
        }

        @Override protected void onFocusChanged(boolean g, int d, Rect r) {
            super.onFocusChanged(g, d, r);
            animate().cancel();
            animate().scaleX(g ? 1.18f : 1f).scaleY(g ? 1.18f : 1f).translationZ(g ? dp(24) : 0)
                .setDuration(g ? 260 : 180)
                .setInterpolator(g ? new OvershootInterpolator(1.6f) : new DecelerateInterpolator())
                .start();
            bg.setColor(g ? 0x33FFFFFF : 0x1AFFFFFF);
            bg.setStroke(dp(g ? 2 : 1), g ? 0xFFFFFFFF : 0x22FFFFFF);
            if (g) {
                label.setText(name);
                label.animate().alpha(1f).setDuration(200).start();
            }
        }
    }

    void launch(String pkg) {
        try {
            Intent i;
            if (pkg == null) i = new Intent(Settings.ACTION_SETTINGS);
            else {
                i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
            }
            if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
            else Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not open", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onResume() { super.onResume(); h.removeCallbacks(tick); h.post(tick); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(tick); }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); dock.getChildAt(0).requestFocus(); }
    @Override public void onBackPressed() { /* a launcher never exits */ }

    @Override public void onWindowFocusChanged(boolean f) {
        super.onWindowFocusChanged(f);
        if (f) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
