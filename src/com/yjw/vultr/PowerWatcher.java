package com.yjw.vultr;
import android.os.*;
import java.util.Iterator;
import org.json.*;
// Only GET observations; all power POSTs require a separate user confirmation.
class PowerWatcher {
 final MainActivity a;final Handler timer=new Handler(Looper.getMainLooper());boolean loading,watching;long next;
 PowerWatcher(MainActivity a){this.a=a;}
 void pause(){timer.removeCallbacks(tick);}void resume(){pause();timer.post(tick);}
 boolean active(){return a.indicatorForeground&&!a.isDestroyed()&&(a.tab.equals("总览")||a.tab.equals("服务器")||a.tab.equals("服务器详情"));}
 boolean pending(){JSONObject all=a.manager.data.optJSONObject("power_pending");if(all==null)return false;for(Iterator<String> it=all.keys();it.hasNext();)if(ServerPower.pending(all.optJSONObject(it.next()),System.currentTimeMillis()))return true;return false;}
 final Runnable tick=new Runnable(){public void run(){if(!active())return;if(pending()){watching=true;if(!a.loading("instances")&&SystemClock.elapsedRealtime()>=next)fetch();timer.postDelayed(this,1000);}else if(watching){watching=false;ServerControls.redraw(a);}}};
 void fetch(){next=SystemClock.elapsedRealtime()+ServerPower.INTERVAL;long ticket=a.epoch;a.load("instances",()->ServerControls.refresh(a.manager),()->{if(active()&&ticket==a.epoch&&!a.loadErrors.containsKey("instances"))ServerControls.redraw(a);});}
}
