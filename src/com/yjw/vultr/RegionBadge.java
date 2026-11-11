package com.yjw.vultr;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import org.json.JSONObject;
import java.time.Instant;
import java.time.ZoneId;
// Region strip on server cards: flag, city and country, region code, and the region's live local time.
final class RegionBadge {
 private RegionBadge(){}
 static void add(MainActivity a,LinearLayout parent,JSONObject x){
  String id=x.optString("region"),zone=Regions.zone(id),title=Regions.title(id);
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(a.dp(12),a.dp(8),a.dp(12),a.dp(8));row.setBackground(UiSkin.shape(a.SOFT,0,a.dp(8)));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=a.dp(8);lp.bottomMargin=a.dp(4);parent.addView(row,lp);
  TextView flag=new TextView(a);flag.setText(Regions.flag(id));flag.setTextSize(26);flag.setTextColor(a.INK);flag.setImportantForAccessibility(TextView.IMPORTANT_FOR_ACCESSIBILITY_NO);
  LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-2,-2);fp.setMarginEnd(a.dp(12));row.addView(flag,fp);
  LinearLayout text=new LinearLayout(a);text.setOrientation(1);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);text.addView(top);
  TextView name=a.label(title,16,a.INK);name.setTypeface(MainActivity.MEDIUM);name.setPadding(0,0,0,0);top.addView(name);
  if(zone!=null){TextView code=a.mono(a.label(Regions.code(id),12,a.MUTED));code.setPadding(a.dp(8),0,0,0);top.addView(code);}
  if(zone==null){row.setContentDescription("地区 "+title);return;}
  // TextClock keeps the minute current by itself, so the card needs no redraw for the time.
  LinearLayout bottom=new LinearLayout(a);bottom.setGravity(Gravity.CENTER_VERTICAL);text.addView(bottom);
  TextClock clock=new TextClock(a);clock.setTimeZone(zone);clock.setFormat24Hour("'当地' HH:mm E");clock.setFormat12Hour("'当地' HH:mm E");clock.setTextSize(13);clock.setTextColor(a.INK);clock.setFontFeatureSettings("tnum");clock.setPadding(0,a.dp(2),0,0);bottom.addView(clock);
  TextView gap=a.label(" · "+Regions.offset(id,ZoneId.systemDefault(),Instant.now()),12,a.MUTED);gap.setPadding(0,a.dp(2),0,0);gap.setSingleLine(true);gap.setEllipsize(android.text.TextUtils.TruncateAt.END);bottom.addView(gap,new LinearLayout.LayoutParams(0,-2,1));
  name.setContentDescription("地区 "+title);
 }
}
