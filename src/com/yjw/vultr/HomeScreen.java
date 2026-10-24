package com.yjw.vultr;
import android.os.*;
import android.widget.*;
import java.util.*;
import org.json.*;
final class HomeScreen {
 final MainActivity a;final Handler timer=new Handler(Looper.getMainLooper());boolean visible,foreground=true,autoRefresh=true,motion=true;int generation;JSONArray snapshot=new JSONArray();long checked;int freshness=-1;String error="",accountError="";TextView updated;final List<PulseView> pulses=new ArrayList<>();final List<TextView> badges=new ArrayList<>();final List<JSONObject> shown=new ArrayList<>();
 HomeScreen(MainActivity a){this.a=a;}
 void open(){visible=true;foreground=true;generation++;snapshot=a.manager.data.optJSONArray("home_snapshot");if(snapshot==null)snapshot=new JSONArray();checked=a.manager.data.optLong("home_checked");autoRefresh=a.manager.data.optBoolean("ui_auto_refresh",true);motion=a.manager.data.optBoolean("ui_motion",true);render();timer.removeCallbacks(tick);timer.post(tick);}
 boolean active(){return visible&&foreground&&!a.isFinishing()&&!a.isDestroyed()&&a.tab.equals("总览");}
 void leave(){visible=false;generation++;timer.removeCallbacks(tick);stop();}void pause(){foreground=false;timer.removeCallbacks(tick);stop();}void resume(){foreground=true;if(visible){timer.removeCallbacks(tick);timer.post(tick);}}void stop(){for(PulseView p:pulses)p.foreground(false);}
 final Runnable tick=new Runnable(){public void run(){if(!active())return;if(freshness!=freshness())renderLater();updateStatus();if(autoRefresh)refresh(false);timer.postDelayed(this,1000);}};
 void refreshAll(){refresh(true);updateStatus();}
 // Tiered: servers 60s, balance 5 min, Cloudflare 15 min; the three run in parallel and never lock buttons.
 void refresh(boolean force){if(!active())return;Manager m=a.manager;Runnable done=()->{if(active())renderLater();};
  JSONObject acct=m.data.optJSONObject("account_summary"),cloud=CloudMonitor.snapshot(m);
  if(force)a.load("account",()->AccountSummary.fetch(m),done);else a.loadIfDue("account",Refresher.ACCOUNT,acct==null?0:acct.optLong("checked"),()->AccountSummary.fetch(m),done);
  if(CloudPublisher.config(m)!=null){if(force)a.load("cloud",()->CloudMonitor.check(m,"home"),done);else a.loadIfDue("cloud",Refresher.CLOUD,cloud==null?0:cloud.optLong("checked"),()->CloudMonitor.check(m,"home"),done);}
  if(force)a.load("instances",()->m.refreshInstances(),done);else a.loadIfDue("instances",Refresher.INSTANCES,m.data.optLong("home_checked"),()->m.refreshInstances(),done);
 }
 String fingerprint(){Object acct=a.manager.data.opt("account_summary"),cloud=a.manager.data.opt("cloud_monitor");return a.manager.data.optLong("home_checked")+"|"+a.manager.data.opt("home_snapshot")+"|"+acct+"|"+(cloud instanceof JSONObject?((JSONObject)cloud).optLong("checked"):0)+"|"+freshness()+"|"+a.loadErrors.get("account")+a.loadErrors.get("instances");}
 void renderLater(){a.renderLater("home",this::fingerprint,this::render);}
 void render(){if(!active())return;int y=a.scroll().getScrollY();snapshot=a.manager.data.optJSONArray("home_snapshot");if(snapshot==null)snapshot=new JSONArray();checked=a.manager.data.optLong("home_checked");error=a.loadErrors.containsKey("instances")?"服务器更新失败 · 保留缓存":"";accountError=a.loadErrors.containsKey("account")?"余额更新失败，保留上次金额":"";freshness=freshness();a.rendered("home",fingerprint());stop();pulses.clear();badges.clear();shown.clear();a.body.removeAllViews();AccountSummary.card(a,accountError,true);
  updated=a.section("服务器 · "+snapshot.length(),"");
  if(snapshot.length()>0){try{hero(snapshot.getJSONObject(0));}catch(Exception e){a.card("实例信息不完整","请刷新后重试。");}}else{LinearLayout c=a.card(checked==0?"尚未获取服务器":"暂无服务器","新建前可查看方案价格与实时库存。");a.button(c,"选择地区与套餐",()->new StockScreen(a).open());}
  a.section("订阅","");CloudMonitor.card(a,()->render(),true);a.section("快捷操作","");LinearLayout quick=a.card("","");quick.setPadding(a.dp(4),a.dp(4),a.dp(4),a.dp(4));a.row(quick,"新建服务器","选择地区与套餐",()->new StockScreen(a).open());a.row(quick,"管理订阅","",()->a.page("订阅"));a.row(quick,"费用明细","",()->a.page("费用"));updateStatus();a.restoreScroll(y);
 }
 int freshness(){JSONObject cloud=CloudMonitor.snapshot(a.manager);int mask=AccountSummary.fresh(a.manager.data.optJSONObject("account_summary"),System.currentTimeMillis())?1:0;if(cloud!=null&&CloudState.fresh(cloud.optLong("checked"),System.currentTimeMillis()))mask|=2;if(HomeState.fresh(checked,System.currentTimeMillis()))mask|=4;return mask;}
 void hero(JSONObject x)throws Exception{LinearLayout c=a.card("","");LinearLayout row=new LinearLayout(a);row.setGravity(android.view.Gravity.CENTER_VERTICAL);c.addView(row);LinearLayout text=new LinearLayout(a);text.setOrientation(1);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMarginEnd(a.dp(12));row.addView(text,lp);TextView name=a.label(x.optString("label","未命名服务器"),16,a.INK);name.setTypeface(MainActivity.MEDIUM);text.addView(name);TextView badge=a.pill(text,"",a.MUTED);text.addView(a.mono(a.label(x.optString("main_ip","等待分配 IP"),13,a.INK)));SystemBadge.system(a,text,x);PulseView pulse=new PulseView(a,a.manager.data.optString("ui_server_animation"));row.addView(pulse,new LinearLayout.LayoutParams(a.dp(80),a.dp(64)));pulses.add(pulse);badges.add(badge);shown.add(x);SystemBadge.host(a,c,x);c.addView(a.tnum(a.label(x.optString("region").toUpperCase(Locale.ROOT)+" · "+x.optString("vcpu_count","—")+" vCPU · "+x.optString("ram","—")+" MB · "+x.optString("disk","—")+" GB  ·  创建于 "+Core.shortTime(x.optString("date_created")),12,a.MUTED)));ServerControls.add(a,c,x,true);}
 void updateStatus(){boolean fresh=error.isEmpty()&&HomeState.fresh(checked,System.currentTimeMillis());if(updated!=null)a.setTextIfChanged(updated,!error.isEmpty()?error:checked==0?"等待首次查询":(fresh?"":"缓存 · ")+Core.shortTime(checked)+" 更新"+(a.loading("instances")?" · 更新中…":"")+(!autoRefresh?" · 自动暂停":""));for(int i=0;i<shown.size();i++){JSONObject x=shown.get(i);String kind=BootState.kind(x.optString("status"),x.optString("power_status"),x.optString("server_status"),fresh);boolean run=kind.equals("running");a.pillStyle(badges.get(i),(run?"● ":"")+BootState.label(kind),run?a.GREEN:BootState.starting(kind)?a.AMBER:a.MUTED);if(i<pulses.size())pulses.get(i).state(kind,active()&&motion);}}
 void details(JSONObject x){new ServerScreen(a).detail(x);}
}
