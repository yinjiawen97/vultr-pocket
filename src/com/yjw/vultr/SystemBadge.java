package com.yjw.vultr;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
// Static local vectors. Text is the accessible source of truth; the icon is decorative.
final class SystemBadge {
 static void system(MainActivity a,LinearLayout parent,JSONObject x){
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
  ImageView icon=new ImageView(a);icon.setImageDrawable(new Glyph(ServerIdentity.family(x.optString("os")),a.dark,a.MUTED));icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams slot=new LinearLayout.LayoutParams(a.dp(20),a.dp(20));slot.setMarginEnd(a.dp(8));row.addView(icon,slot);
  TextView name=a.label(ServerIdentity.name(x.optString("os")),14,a.MUTED);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
 }
 static void host(MainActivity a,LinearLayout parent,JSONObject x){parent.addView(a.label(ServerIdentity.host(x.optString("plan")),14,a.MUTED));}
 static void add(MainActivity a,LinearLayout parent,JSONObject x){system(a,parent,x);host(a,parent,x);}
 static final class Glyph extends Drawable {
  final Paint p=new Paint(3);final Path path=new Path(),detail=new Path();final String family;final int ink,accent,cutout;int alpha=255;
  Glyph(String family,boolean dark,int muted){this.family=family;ink=muted;cutout=dark?Color.parseColor("#111318"):Color.WHITE;accent=Color.parseColor(family.equals("ubuntu")?(dark?"#FFA67D":"#B6400A"):family.equals("debian")?(dark?"#FF8AB4":"#B71950"):family.equals("windows")?(dark?"#8EC5FF":"#0866AD"):family.equals("fedora")?(dark?"#9BBFFF":"#315999"):family.equals("rocky")||family.equals("alma")?(dark?"#7FDCBE":"#147254"):family.equals("arch")?(dark?"#8DCDF4":"#126F9E"):dark?"#D4DEEC":"#394B62");
   if(family.equals("macos")){path.moveTo(17,12);path.cubicTo(17,9,19,8,19,8);path.cubicTo(17,5,14,6,12,7);path.cubicTo(9,5,5,6,4,10);path.cubicTo(3,14,6,21,9,21);path.cubicTo(11,21,11,20,13,20);path.cubicTo(15,20,15,21,17,21);path.cubicTo(19,20,20,17,20,16);path.cubicTo(18,16,17,14,17,12);path.close();detail.moveTo(12,6);detail.cubicTo(12,3,14,2,17,2);detail.cubicTo(17,4,15,6,12,6);detail.close();}
   if(family.equals("linux")){path.moveTo(7,10);path.cubicTo(4,13,3,17,5,19);path.lineTo(19,19);path.cubicTo(21,17,20,13,17,10);path.cubicTo(18,3,15,2,12,2);path.cubicTo(9,2,6,4,7,10);path.close();}
   if(family.equals("arch")){path.moveTo(12,2);path.lineTo(22,22);path.lineTo(15,19);path.cubicTo(15,12,9,12,9,19);path.lineTo(2,22);path.lineTo(12,2);path.close();}
   if(family.equals("debian")){path.moveTo(11,13);path.cubicTo(6,13,8,6,13,7);path.cubicTo(19,8,17,16,11,17);path.cubicTo(3,17,2,7,8,4);path.cubicTo(16,-1,23,6,20,13);path.cubicTo(17,21,6,22,3,18);}
  }
  void fill(int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setAlpha(alpha);}
  void stroke(int color,float width){fill(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
  public void draw(Canvas c){Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);fill(accent);
   switch(family){
    case "ubuntu":stroke(accent,2.8f);for(int i=0;i<3;i++)c.drawArc(5,5,19,19,i*120+17,86,false,p);fill(accent);for(int i=0;i<3;i++){double angle=i*Math.PI*2/3;c.drawCircle(12+9*(float)Math.cos(angle),12+9*(float)Math.sin(angle),2.5f,p);}break;
    case "debian":stroke(accent,1.7f);c.drawPath(path,p);break;
    case "windows":c.drawRect(3,4,11,11,p);c.drawRect(13,4,21,11,p);c.drawRect(3,13,11,20,p);c.drawRect(13,13,21,20,p);break;
    case "macos":c.drawPath(path,p);c.drawPath(detail,p);break;
    case "fedora":c.drawCircle(12,12,10,p);stroke(cutout,2.3f);c.drawLine(12,19,12,7,p);c.drawArc(12,4,18,10,180,180,false,p);c.drawLine(8,11,16,11,p);break;
    case "rocky":c.drawCircle(12,12,10,p);stroke(cutout,2.7f);c.drawLine(5,16,12,9,p);c.drawLine(12,9,16,13,p);c.drawLine(16,13,20,9,p);break;
    case "alma":for(int i=0;i<4;i++){c.save();c.rotate(i*90,12,12);c.drawOval(5,2,12,12,p);c.restore();}break;
    case "arch":c.drawPath(path,p);break;
    case "linux":c.drawPath(path,p);fill(cutout);c.drawOval(7,10,17,20,p);c.drawCircle(10,7,1.3f,p);c.drawCircle(14,7,1.3f,p);fill(accent);c.drawCircle(10,7,.5f,p);c.drawCircle(14,7,.5f,p);stroke(accent,1.8f);c.drawLine(9,10,12,11,p);c.drawLine(12,11,15,10,p);c.drawLine(4,21,9,20,p);c.drawLine(15,20,20,21,p);break;
    default:stroke(ink,1.6f);c.drawRoundRect(3,4,21,17,2,2,p);c.drawLine(12,17,12,21,p);c.drawLine(8,21,16,21,p);c.drawLine(7,9,10,11,p);c.drawLine(10,11,7,13,p);c.drawLine(13,13,17,13,p);
   }c.restore();
  }
  public void setAlpha(int value){alpha=value;invalidateSelf();}public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
  public int getIntrinsicWidth(){return 24;}public int getIntrinsicHeight(){return 24;}
 }
}
