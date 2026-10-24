package com.yjw.vultr;
import android.view.View;
import android.content.Context;
import android.graphics.*;
import android.animation.ValueAnimator;
import android.os.*;
import android.database.ContentObserver;
import android.provider.Settings;
// Decorative cloud power state. Fan speed and LED brightness are never telemetry.
final class PulseView extends View {
 final Paint paint=new Paint(3);final Rect clip=new Rect();final Path top=new Path(),side=new Path(),blade=new Path();
 final RectF front=new RectF(5,18,75,51),panel=new RectF(8,21,72,48),shadow=new RectF(8,21,77,55);
 final int green,amber,muted,border,highlight,bladeColor,well;final LinearGradient metal,upper,edge;final RadialGradient hub;
 final PowerManager power;final Handler handler=new Handler(Looper.getMainLooper());final ContentObserver scales;
 String style;boolean running,starting,booting,live,hostActive,posted,observing,forceReduced;long frames;
 final Runnable frame=new Runnable(){public void run(){posted=false;if(shouldAnimate()){frames++;invalidate();}else invalidate();}};
 PulseView(Context c){this(c,"pulse");}
 PulseView(Context c,String value){super(c);style=IndicatorState.style(value);boolean dark=(getResources().getConfiguration().uiMode&48)==32;
  amber=Color.parseColor(dark?"#FFD087":"#955000");green=Color.parseColor(dark?"#71E1B5":"#087754");muted=Color.parseColor(dark?"#84909F":"#647181");border=Color.parseColor(dark?"#0C1118":"#697B8F");highlight=Color.parseColor(dark?"#9AAABC":"#FFFFFF");bladeColor=Color.parseColor(dark?"#91A2B3":"#A8B9C9");well=Color.parseColor(dark?"#0F1720":"#243443");
  metal=new LinearGradient(0,18,0,51,new int[]{Color.parseColor(dark?"#627181":"#E9EFF5"),Color.parseColor(dark?"#394756":"#C0CDD9"),Color.parseColor(dark?"#273440":"#9EADBD")},null,Shader.TileMode.CLAMP);
  upper=new LinearGradient(0,9,0,19,new int[]{Color.parseColor(dark?"#8896A6":"#F7FAFD"),Color.parseColor(dark?"#4B596A":"#CED8E4")},null,Shader.TileMode.CLAMP);
  edge=new LinearGradient(69,0,78,0,new int[]{Color.parseColor(dark?"#3D4A5B":"#B4C1D0"),Color.parseColor(dark?"#17212D":"#77889B")},null,Shader.TileMode.CLAMP);
  hub=new RadialGradient(-1,-1,3,new int[]{Color.parseColor("#DAE4EE"),Color.parseColor("#61758B")},null,Shader.TileMode.CLAMP);
  top.moveTo(5,18);top.lineTo(12,9);top.lineTo(70,9);top.lineTo(77,17);top.lineTo(75,20);top.close();side.moveTo(75,18);side.lineTo(77,17);side.lineTo(77,47);side.lineTo(75,51);side.close();
  blade.moveTo(1,-1);blade.cubicTo(3,-8,8,-8,8,-4);blade.cubicTo(8,-1,4,1,1,1);blade.close();
  power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);hostActive=!(c instanceof MainActivity)||((MainActivity)c).indicatorForeground;
  scales=new ContentObserver(handler){public void onChange(boolean self){invalidate();sync();}};
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
 }
 void state(boolean value,boolean motion){state(value?"running":"unknown",motion);}
 void state(String kind,boolean motion){boolean run=kind.equals("running"),start=BootState.starting(kind),boot=kind.equals("booting");boolean changed=running!=run||starting!=start||booting!=boot||live!=motion;running=run;starting=start;booting=boot;live=motion;if(changed)invalidate();sync();}
 void foreground(boolean active){hostActive=active;invalidate();sync();}
 void reducedForPreview(boolean value){forceReduced=value;invalidate();sync();}
 boolean shouldAnimate(){boolean base=IndicatorState.animate(running||starting,live,hostActive,isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE&&hasWindowFocus(),!forceReduced&&ValueAnimator.areAnimatorsEnabled(),power!=null&&power.isInteractive());return base&&getGlobalVisibleRect(clip)&&!clip.isEmpty();}
 void sync(){if(frame==null)return;if(shouldAnimate()){if(!posted){posted=true;postDelayed(frame,IndicatorState.FRAME_MS);}}else{removeCallbacks(frame);posted=false;}}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();Context c=getContext();if(c instanceof MainActivity){MainActivity a=(MainActivity)c;a.indicators.add(this);hostActive=a.indicatorForeground;}getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,scales);observing=true;sync();}
 @Override protected void onDetachedFromWindow(){removeCallbacks(frame);posted=false;if(observing){getContext().getContentResolver().unregisterContentObserver(scales);observing=false;}if(getContext() instanceof MainActivity)((MainActivity)getContext()).indicators.remove(this);super.onDetachedFromWindow();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(frame!=null){invalidate();sync();}}
 @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(frame!=null)sync();}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);invalidate();sync();}
 void fill(Canvas c,int color){paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setColor(color);}
 void screw(Canvas c,float x,float y){fill(c,border);c.drawCircle(x,y,1.1f,paint);paint.setColor(highlight);paint.setStrokeWidth(.4f);c.drawLine(x-.65f,y-.4f,x+.65f,y-.4f,paint);}
 void fan(Canvas c,float x,float y,float angle){c.save();c.translate(x,y);fill(c,well);c.drawCircle(0,0,10.3f,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.8f);paint.setColor(border);c.drawCircle(0,0,10.5f,paint);fill(c,bladeColor);c.save();c.rotate(angle);for(int i=0;i<4;i++){c.drawPath(blade,paint);c.rotate(90);}c.restore();paint.setShader(hub);c.drawCircle(0,0,2.8f,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.5f);paint.setColor(highlight);paint.setAlpha(95);c.drawCircle(0,0,8.5f,paint);c.drawCircle(0,0,6.6f,paint);paint.setAlpha(150);c.drawLine(-9.5f,0,-3,0,paint);c.drawLine(3,0,9.5f,0,paint);c.drawLine(0,-9.5f,0,-3,paint);c.drawLine(0,3,0,9.5f,paint);c.restore();}
 void rack(Canvas c,boolean moving,long clock){fill(c,Color.argb(18,0,0,0));c.drawRoundRect(shadow,4,4,paint);paint.setAlpha(255);paint.setShader(upper);c.drawPath(top,paint);paint.setShader(edge);c.drawPath(side,paint);paint.setShader(metal);c.drawRoundRect(front,2.8f,2.8f,paint);paint.setShader(null);paint.setColor(border);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.65f);c.drawRoundRect(front,2.8f,2.8f,paint);paint.setColor(highlight);paint.setAlpha(130);c.drawLine(8,19,72,19,paint);c.drawLine(13,11,68,11,paint);fill(c,Color.argb(25,0,0,0));c.drawRoundRect(panel,1.8f,1.8f,paint);
  float angle=moving&&(running||booting)?(clock%6400)/6400f*360:25;fan(c,25,34.5f,angle);fan(c,49,34.5f,angle+45);screw(c,9,23);screw(c,71,23);screw(c,9,47);screw(c,71,47);
  fill(c,border);for(int i=0;i<4;i++)c.drawRoundRect(62,33+i*2.6f,68,34.1f+i*2.6f,.5f,.5f,paint);int lamp=starting?amber:running?green:muted;float wave=moving?.5f+.5f*(float)Math.sin(clock/2400f*Math.PI*2):.5f;fill(c,lamp);paint.setAlpha(running||starting?(int)(30+45*wave):25);c.drawCircle(65,27.5f,4,paint);paint.setAlpha(255);c.drawCircle(65,27.5f,1.9f,paint);paint.setColor(highlight);paint.setAlpha(200);c.drawCircle(64.5f,27,.6f,paint);paint.setAlpha(255);
 }
 protected void onDraw(Canvas c){super.onDraw(c);boolean moving=shouldAnimate();long clock=(SystemClock.uptimeMillis()/IndicatorState.FRAME_MS)*IndicatorState.FRAME_MS;float scale=Math.min(getWidth()/80f,getHeight()/64f);if(scale<=0)return;c.save();c.translate((getWidth()-80*scale)/2,(getHeight()-64*scale)/2);c.scale(scale,scale);if(style.equals("rack")){rack(c,moving,clock);if(starting){for(int i=0;i<3;i++){fill(c,amber);paint.setAlpha(!moving||i==(clock/450)%3?230:65);c.drawCircle(29+11*i,58,1.5f,paint);}}}else{float t=moving?(clock%2400)/2400f:0;fill(c,starting?amber:running?green:muted);c.drawCircle(40,32,4,paint);if(moving){if(starting){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);for(int i=0;i<3;i++)c.drawArc(30,22,50,42,(clock%3000)/3000f*360+i*120,55,false,paint);}else{paint.setAlpha((int)(65*(1-t)));c.drawCircle(40,32,5+9*t,paint);}}}c.restore();sync();}
}
