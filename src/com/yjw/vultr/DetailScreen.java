package com.yjw.vultr;
import android.widget.*;
import org.json.*;
final class DetailScreen {
 final MainActivity a;DetailScreen(MainActivity a){this.a=a;}
 void render(JSONObject r){
  a.body.removeAllViews(); // Refresh replaces this detail page; never append another copy.
  LinearLayout summary=a.card(r.optString("label","部署详情"),"");SystemBadge.add(a,summary,r);summary.addView(a.label(UiSkin.phase(r.optString("phase")),14,a.MUTED));a.fact(summary,"公网 IP",r.optString("ip","尚未分配"));LinearLayout startup=new LinearLayout(a);startup.setGravity(android.view.Gravity.CENTER_VERTICAL);summary.addView(startup);LinearLayout words=new LinearLayout(a);words.setOrientation(1);startup.addView(words,new LinearLayout.LayoutParams(0,-2,1));TextView state=a.label("",16,a.MUTED);words.addView(state);TextView note=a.label("",12,a.MUTED);words.addView(note);PulseView effect=new PulseView(a,a.manager.data.optString("ui_server_animation"));LinearLayout.LayoutParams device=new LinearLayout.LayoutParams(a.dp(80),a.dp(64));device.setMarginStart(a.dp(12));startup.addView(effect,device);if(a.boot==null)a.boot=new BootWatcher(a);a.boot.bind(r,state,note,effect);
  ServerControls.add(a,summary,r,false);a.row(summary,"刷新服务器信息",a.loading("info:"+r.optString("id"))?"更新中…":"",()->{long ticket=a.epoch;a.load("info:"+r.optString("id"),()->a.manager.refreshInfo(r),()->{if(a.epoch!=ticket||!a.tab.equals("服务器详情"))return;if(a.loadErrors.containsKey("info:"+r.optString("id"))){a.toast("刷新失败，保留上次信息");return;}if(a.boot!=null)a.boot.refreshed();a.showDeployment(r);});});a.row(summary,"更多管理","",()->{try{JSONObject target=ServerPower.safe(r).put("main_ip",r.optString("ip"));new ServerScreen(a).more(target);}catch(Exception e){a.message("实例记录不完整，请刷新重试。");}});

  LinearLayout dates=a.card("部署时间", "时间按手机时区显示 · "+java.time.ZoneId.systemDefault().getId());a.fact(dates,"最初创建",Core.time(r.optString("date_created")));a.fact(dates,r.optString("mode").equals("reinstall")?"本次重装请求":"本次部署请求",Core.time(r.optString("requested_at")));a.fact(dates,"首次记录的订阅核验通过",Core.time(r.optString("ready_verified_at")));a.fact(dates,"最近查询",Core.time(r.optString("checked_at")));
  LinearLayout subscription=a.card("订阅与检查",r.optString("note","订阅下载和代理出口需分别核验。"));if(r.has("publish_note"))subscription.addView(a.label(r.optString("publish_note"),14,a.MUTED));
  if(r.has("url")&&!Core.inactive(r.optString("phase")))a.pair(subscription,"导入到 FlClash",MainActivity.Tone.PRIMARY,()->SubscriptionActions.flclash(a,r),"更多",()->a.menu("订阅操作",new String[]{"用于电脑 Clash Verge","复制订阅链接","查看私密订阅地址","收藏与完整节点检测","检查安装 / 核验订阅","等待检查（最多 3 分钟）"},()->SubscriptionActions.desktop(a,r),()->a.copySubscription(r),()->a.message(r.optString("url")),()->a.page("订阅"),()->a.check(r),()->a.poll(r)));else a.pair(subscription,"核验订阅",()->a.check(r),"等待检查",()->a.poll(r));
  LinearLayout hardware=a.card("服务器配置","");a.fact(hardware,"地区 / 套餐",r.optString("region","待刷新")+" / "+r.optString("plan","待刷新"));a.fact(hardware,"硬件",r.optString("vcpu_count","?")+" vCPU · "+r.optString("ram","?")+" MB 内存 · "+r.optString("disk","?")+" GB 磁盘");a.fact(hardware,"实例 ID",r.optString("id","请求结果待核对"));
  a.helpCard("时间与检测范围","云端创建时间不代表最近重装时间。首次核验时间不代表安装实际完成时间。旧本机时间未记录时显示尚无记录。\n订阅核验只确认下载内容；不代表代理出口、UDP 或 AI 账号已通过测试。等待失败不会自动删除服务器。");
 }
}
