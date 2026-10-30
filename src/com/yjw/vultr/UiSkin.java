package com.yjw.vultr;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.animation.*;
import android.view.View;
import android.view.animation.PathInterpolator;
final class UiSkin {
 static GradientDrawable shape(int fill,int border,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(radius);if(border!=0)d.setStroke(1,border);return d;}
 static RippleDrawable ripple(int fill,int border,int radius,int ink){return new RippleDrawable(ColorStateList.valueOf((ink&0x00ffffff)|0x22000000),shape(fill,border,radius),null);}
 // Strong ease-out: fast start so a press or a new page answers immediately.
 static final TimeInterpolator EASE_OUT=new PathInterpolator(0.23f,1f,0.32f,1f);
 // Press feedback: shrink on touch-down, spring back quicker than it went in. System "remove animations" zeroes the durations.
 static void press(View v,float scale){StateListAnimator s=new StateListAnimator();s.addState(new int[]{android.R.attr.state_pressed,android.R.attr.state_enabled},scaleTo(v,scale,100));s.addState(new int[0],scaleTo(v,1f,160));v.setStateListAnimator(s);}
 static Animator scaleTo(View v,float scale,long ms){AnimatorSet set=new AnimatorSet();set.playTogether(ObjectAnimator.ofFloat(v,"scaleX",scale),ObjectAnimator.ofFloat(v,"scaleY",scale));set.setDuration(ms);set.setInterpolator(EASE_OUT);return set;}
 static void haptic(View v){v.performHapticFeedback(android.os.Build.VERSION.SDK_INT>=30?android.view.HapticFeedbackConstants.CONFIRM:android.view.HapticFeedbackConstants.CONTEXT_CLICK);}
 static String phase(String value){switch(value){case "subscription-ready":return "订阅已就绪";case "created":return "云端已创建";case "create-pending":return "创建结果待核对";case "reinstall-preparing":return "准备重装";case "reinstall-pending":return "重装结果待核对";case "deleted":return "已删除";case "create-rejected":return "创建未通过";default:return value.isEmpty()?"尚未检查":value;}}
 static final class Icon extends Drawable {
  final Paint p=new Paint(3);final int kind,color;Icon(int kind,int color){this.kind=kind;this.color=color;}
  public void draw(Canvas canvas){Rect b=getBounds();canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path path=new Path();
   switch(kind){case 0:path.moveTo(3,10);path.lineTo(12,3);path.lineTo(21,10);path.moveTo(5,9);path.lineTo(5,21);path.lineTo(10,21);path.lineTo(10,15);path.lineTo(14,15);path.lineTo(14,21);path.lineTo(19,21);path.lineTo(19,9);canvas.drawPath(path,p);break;
   case 1:canvas.drawRoundRect(4,3,20,10,2,2,p);canvas.drawRoundRect(4,14,20,21,2,2,p);canvas.drawLine(8,6,8,7,p);canvas.drawLine(8,17,8,18,p);canvas.drawLine(12,6.5f,16,6.5f,p);canvas.drawLine(12,17.5f,16,17.5f,p);break;
   case 2:path.moveTo(9,15);path.lineTo(15,9);canvas.drawPath(path,p);canvas.drawArc(2,8,14,20,45,270,false,p);canvas.drawArc(10,0,22,12,225,270,false,p);break;
   case 3:canvas.drawRoundRect(5,2,19,22,2,2,p);canvas.drawLine(9,7,15,7,p);canvas.drawLine(9,11,15,11,p);canvas.drawLine(9,15,13,15,p);break;
   case 5:canvas.drawArc(4,4,20,20,-50,285,false,p);path.moveTo(15,3);path.lineTo(20,4);path.lineTo(20,9);canvas.drawPath(path,p);break;
   case 6:path.moveTo(15,4);path.lineTo(7,12);path.lineTo(15,20);canvas.drawPath(path,p);break;
   default:canvas.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double angle=i*Math.PI/4;canvas.drawLine(12+(float)Math.cos(angle)*7,12+(float)Math.sin(angle)*7,12+(float)Math.cos(angle)*9,12+(float)Math.sin(angle)*9,p);}canvas.drawCircle(12,12,7,p);
   }canvas.restore();}
  public int getIntrinsicWidth(){return 24;}public int getIntrinsicHeight(){return 24;}
  public void setAlpha(int alpha){p.setAlpha(alpha);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
}
