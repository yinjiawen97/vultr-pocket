package com.yjw.vultr;
import android.view.*;
import android.widget.*;
// In-app status strip floating above the bottom navigation; replaces the grey system Toast while the app is in front.
final class Notice extends LinearLayout {
 enum Kind{INFO,SUCCESS,ERROR}
 final MainActivity a;final ImageView icon;final TextView text;final Runnable hide=this::dismiss;
 Notice(MainActivity a){super(a);this.a=a;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setPadding(a.dp(14),a.dp(10),a.dp(14),a.dp(10));setBackground(UiSkin.shape(a.SURFACE,a.BORDER,a.dp(12)));setElevation(a.dp(6));setMinimumHeight(a.dp(48));
  icon=new ImageView(a);icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);LayoutParams ip=new LayoutParams(a.dp(20),a.dp(20));ip.setMarginEnd(a.dp(10));addView(icon,ip);
  text=a.label("",14,a.INK);text.setMaxLines(3);text.setEllipsize(android.text.TextUtils.TruncateAt.END);addView(text,new LayoutParams(0,-2,1));
  setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);setVisibility(GONE);setOnClickListener(v->dismiss());}
 // A new message replaces the current one and restarts its timer; nothing queues behind it.
 void show(String message,Kind kind){removeCallbacks(hide);animate().cancel();icon.setImageDrawable(new UiSkin.Icon(kind==Kind.SUCCESS?7:kind==Kind.ERROR?8:9,kind==Kind.SUCCESS?a.GREEN:kind==Kind.ERROR?a.DANGER:a.BLUE));text.setText(message);
  if(getVisibility()!=VISIBLE){setVisibility(VISIBLE);setAlpha(0f);setTranslationY(a.dp(16));}animate().alpha(1f).translationY(0f).setDuration(200).setInterpolator(UiSkin.EASE_OUT).start();postDelayed(hide,message.length()>24?4000:2500);}
 // Leaving is quicker than arriving.
 void dismiss(){removeCallbacks(hide);if(getVisibility()!=VISIBLE)return;animate().cancel();animate().alpha(0f).translationY(a.dp(8)).setDuration(150).setInterpolator(UiSkin.EASE_OUT).withEndAction(()->setVisibility(GONE)).start();}
}
