package com.yjw.vultr;
import android.widget.LinearLayout;
import android.content.Context;
import android.graphics.*;
import android.animation.ValueAnimator;
final class FusedNav extends LinearLayout {
 final Paint paint=new Paint(3);final float d;float position=0;int fill;ValueAnimator motion;
 FusedNav(Context c){super(c);d=getResources().getDisplayMetrics().density;setWillNotDraw(false);}
 void select(int index,int color){fill=color;if(motion!=null)motion.cancel();if(!ValueAnimator.areAnimatorsEnabled()||!isLaidOut()){position=index;invalidate();return;}motion=ValueAnimator.ofFloat(position,index);motion.setDuration(220);motion.setInterpolator(UiSkin.EASE_OUT);motion.addUpdateListener(v->{position=(Float)v.getAnimatedValue();invalidate();});motion.start();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(getChildCount()==0)return;float w=(getWidth()-getPaddingLeft()-getPaddingRight())/(float)getChildCount();float left=getPaddingLeft()+position*w+4*d;paint.setColor(fill);c.drawRoundRect(left,getPaddingTop()+4*d,left+w-8*d,getHeight()-getPaddingBottom()-4*d,22*d,22*d,paint);}
 @Override protected void onDetachedFromWindow(){if(motion!=null)motion.cancel();super.onDetachedFromWindow();}
}
