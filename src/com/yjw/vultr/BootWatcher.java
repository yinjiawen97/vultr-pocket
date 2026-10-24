package com.yjw.vultr;
import android.os.*;
import android.widget.*;
import org.json.JSONObject;
// Foreground-only, bounded GET observer. Never calls check(), publish(), create() or power APIs.
class BootWatcher {
 final MainActivity a;final Handler timer=new Handler(Looper.getMainLooper());JSONObject record;long epoch,started,next;boolean shown,loading,failed,done;TextView status,note;PulseView effect;
 BootWatcher(MainActivity a){this.a=a;}
 boolean active(){return shown&&a.indicatorForeground&&!a.isFinishing()&&!a.isDestroyed()&&a.tab.equals("服务器详情")&&a.epoch==epoch;}
 void bind(JSONObject r,TextView status,TextView note,PulseView effect){
  if(record!=r||!shown){record=r;started=SystemClock.elapsedRealtime();next=0;failed=false;done=false;}
  this.status=status;this.note=note;this.effect=effect;epoch=a.epoch;shown=true;paint();timer.removeCallbacks(tick);if(active())timer.post(tick);
 }
 void leave(){shown=false;timer.removeCallbacks(tick);}void pause(){timer.removeCallbacks(tick);}void resume(){if(shown&&a.tab.equals("服务器详情")){if(!done){started=SystemClock.elapsedRealtime();next=0;}timer.removeCallbacks(tick);timer.post(tick);}}
 void refreshed(){failed=false;done=false;started=SystemClock.elapsedRealtime();next=0;paint();}
 void paint(){if(record==null||status==null)return;String kind=BootState.record(record,System.currentTimeMillis(),failed);boolean run=kind.equals("running");status.setText(run?(record.optString("phase").equals("subscription-ready")?"已开机 · 订阅已核验":"已开机 · 订阅待核验"):BootState.label(kind));status.setTextColor(run?a.GREEN:BootState.starting(kind)?a.AMBER:a.MUTED);effect.state(kind,a.manager.data.optBoolean("ui_motion",true));
  String help=failed?"状态查询失败，保留上次记录；动画已暂停，可点刷新重试。":kind.equals("booting")?"云端报告系统安装 / 启动中；动画不代表实际进度。":kind.equals("creating")?"云端正在准备实例，随后会自动启动。":run?"云端已确认运行；开机不代表订阅或代理出口已可用。":kind.equals("stopped")?"云端报告已关机；观察状态不会自动开机。":"尚无新鲜的云端运行状态，请刷新核对。";
  if(!done&&BootState.observe(kind,record.optString("phase"),!record.optString("id").isEmpty()))help+=SystemClock.elapsedRealtime()-started<BootState.OBSERVE_MS?"\n前台每 10 秒读取状态，最多观察 3 分钟。":"\n本轮自动观察已结束；云端仍会继续启动，可手动刷新。";
  note.setText(help);
 }
 final Runnable tick=new Runnable(){public void run(){if(!active())return;paint();String kind=BootState.record(record,System.currentTimeMillis(),failed);if(kind.equals("running")||kind.equals("stopped")||kind.equals("deleted")||kind.equals("uncertain"))done=true;
  long now=SystemClock.elapsedRealtime();if(!done&&!loading&&now-started<BootState.OBSERVE_MS&&now>=next&&BootState.observe(kind,record.optString("phase"),!record.optString("id").isEmpty()))fetch();timer.postDelayed(this,1000);
 }};
 void fetch(){loading=true;next=SystemClock.elapsedRealtime()+BootState.INTERVAL_MS;JSONObject target=record;long ticket=epoch;String key="boot:"+target.optString("id");a.load(key,()->a.manager.refreshInfo(target),()->{boolean success=!a.loadErrors.containsKey(key);loading=false;if(record!=target||!active()||a.epoch!=ticket)return;failed=!success;if(success){int y=a.scroll().getScrollY();new DetailScreen(a).render(target);a.restoreScroll(y);}else paint();});}
}
