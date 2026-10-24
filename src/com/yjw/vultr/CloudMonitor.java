package com.yjw.vultr;
import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;
import android.widget.*;
final class CloudMonitor {
 static final class HttpError extends IOException {final int code;HttpError(int c){super("HTTP "+c);code=c;}}
 static final class Trace {
  final String stage,url;final long start=System.nanoTime();String phase="setup";int status;Exception error;
  Trace(String stage,String url){this.stage=stage;this.url=url;}
  JSONObject result()throws Exception{return CloudDiagnostics.event(stage,url,phase,status,(System.nanoTime()-start)/1000000,error);}
 }
 static JSONObject get(String url,String token,int max,Trace trace)throws Exception{
  URI u=new URI(url);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new MalformedURLException("需要 HTTPS 地址");
  HttpURLConnection c=(HttpURLConnection)u.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(6000);c.setReadTimeout(10000);c.setRequestProperty("Cache-Control","no-cache");if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
  try{trace.phase="response";trace.status=c.getResponseCode();if(trace.status!=200)throw new HttpError(trace.status);trace.phase="body";return new JSONObject().put("text",Api.read(c.getInputStream(),max));}finally{c.disconnect();}
 }
 static JSONObject network(Manager m)throws Exception{JSONObject x=new JSONObject().put("network","unknown").put("vpn",false).put("validated",false);try{android.net.ConnectivityManager cm=(android.net.ConnectivityManager)m.context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);android.net.Network active=cm.getActiveNetwork();if(active==null)return x.put("network","none");android.net.NetworkCapabilities c=cm.getNetworkCapabilities(active);if(c!=null)x.put("network",c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)?"wifi":c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)?"cellular":c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)?"ethernet":"other").put("vpn",c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)).put("validated",c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED));}catch(Exception ignored){}return x;}
 static int code(Exception e){return e instanceof HttpError?((HttpError)e).code:e instanceof org.json.JSONException||e instanceof IllegalArgumentException?-2:-1;}
 static JSONObject snapshot(Manager m){return m.data.optJSONObject("cloud_monitor");}
 static void check(Manager m)throws Exception{check(m,"manual");}
 static void check(Manager m,String source)throws Exception{
  JSONObject cfg=CloudPublisher.config(m);if(cfg==null)return;JSONObject old=snapshot(m),out=new JSONObject();long start=System.nanoTime();String base=cfg.optString("base").replaceAll("/+$","");
  JSONObject round=network(m).put("source",source).put("version",m.context.getPackageManager().getPackageInfo(m.context.getPackageName(),0).versionName);JSONArray checks=new JSONArray();
  Trace health=new Trace("health",base+"/health");try{String text=get(health.url,null,4096,health).getString("text");health.phase="format";JSONObject h=new JSONObject(text);if(!h.optBoolean("ok"))throw new IllegalArgumentException();out.put("health",200);}catch(Exception e){health.error=e;out.put("health",code(e));}checks.put(health.result());
  Trace admin=new Trace("admin",base+"/api/status");try{String text=get(admin.url,cfg.getString("admin"),16384,admin).getString("text");admin.phase="format";JSONObject s=new JSONObject(text);if(!s.has("published"))throw new IllegalArgumentException();out.put("admin",200).put("published",s.optBoolean("published")).put("nodes",s.optInt("nodes")).put("updated_at",s.optString("updated_at")).put("revision",s.optString("revision"));}catch(Exception e){admin.error=e;out.put("admin",code(e));}checks.put(admin.result());
  Trace subscription=new Trace("subscription",cfg.optString("url"));try{String text=get(subscription.url,null,2*1024*1024,subscription).getString("text");subscription.phase="format";try{Map<String,Object> parsed=Configs.parse(text);out.put("download_nodes",Configs.nodes(parsed).size());Object routing=parsed.get("x-vultr-routing-version");if(routing instanceof Number)out.put("routing_version",((Number)routing).intValue());}catch(Exception invalid){throw new IllegalArgumentException();}out.put("subscription",200);}catch(Exception e){subscription.error=e;out.put("subscription",code(e));}checks.put(subscription.result());
  boolean ok=CloudState.healthy(out.optInt("health"),out.optInt("admin"),out.optInt("subscription"));out.put("checked",System.currentTimeMillis()).put("elapsed_ms",(System.nanoTime()-start)/1000000).put("failures",ok?0:(old==null?1:old.optInt("failures")+1));
  synchronized(m){old=snapshot(m);
  if(old!=null&&old.has("notified")&&!ok)out.put("notified",old.get("notified"));
  JSONArray history=old==null?null:old.optJSONArray("history"),next=new JSONArray();if(history!=null)for(int i=Math.max(0,history.length()-19);i<history.length();i++)next.put(history.get(i));next.put(new JSONObject().put("checked",out.getLong("checked")).put("ok",ok));out.put("history",next);m.data.put("cloud_monitor",out);round.put("checked",out.getLong("checked")).put("elapsed_ms",out.getLong("elapsed_ms")).put("checks",checks);m.data.put("cloud_diagnostics",CloudDiagnostics.append(m.data.optJSONObject("cloud_diagnostics"),round));m.save();}
 }
 static void card(MainActivity a,Runnable refreshed){card(a,refreshed,false);}
 static void card(MainActivity a,Runnable refreshed,boolean compact){JSONObject cfg=CloudPublisher.config(a.manager),s=snapshot(a.manager);long checked=s==null?0:s.optLong("checked");int h=s==null?0:s.optInt("health"),admin=s==null?0:s.optInt("admin"),sub=s==null?0:s.optInt("subscription");boolean fresh=CloudState.fresh(checked,System.currentTimeMillis());
  LinearLayout c=a.card("Cloudflare · 固定订阅","");TextView status=a.label(CloudState.summary(cfg!=null,checked,System.currentTimeMillis(),h,admin,sub),16,cfg!=null&&fresh&&CloudState.healthy(h,admin,sub)?a.GREEN:a.MUTED);c.addView(status);
  if(cfg==null){c.addView(a.label("配置固定入口后，手机可检查 Worker、发布记录和订阅。",14,a.MUTED));a.button(c,"配置固定入口",()->new CloudScreen(a).edit());return;}
  if(compact){if(s!=null&&admin==200)c.addView(a.label(s.optBoolean("published")?s.optInt("nodes")+" 个节点 · 最近发布 "+Core.time(s.optString("updated_at")):"尚未发布配置",12,a.MUTED));if(s!=null)c.addView(a.label("检测于 "+Core.time(java.time.Instant.ofEpochMilli(checked).toString()),12,a.MUTED));a.button(c,"监视详情",MainActivity.Tone.PLAIN,()->new CloudScreen(a).open());return;}
  try{a.fact(c,"服务域名",new URI(cfg.optString("base")).getHost());}catch(Exception ignored){}
  a.fact(c,"Worker 响应",CloudState.stage(h));a.fact(c,"管理权限",CloudState.stage(admin));a.fact(c,"订阅下载 / 格式",CloudState.stage(sub));
  if(s!=null){a.fact(c,"检测时间",Core.time(java.time.Instant.ofEpochMilli(checked).toString()));a.fact(c,"整轮耗时",s.optLong("elapsed_ms")+" ms（非代理延迟）");if(admin==200){a.fact(c,"云端发布",s.optBoolean("published")?"已发布 · "+s.optInt("nodes")+" 个节点":"尚未发布");if(s.optBoolean("published"))a.fact(c,"最近发布时间",Core.time(s.optString("updated_at")));}if(sub==200)a.fact(c,"下载到的节点",s.optInt("download_nodes")+" 个");JSONArray hist=s.optJSONArray("history");if(hist!=null){int ok=0;for(int i=0;i<hist.length();i++)if(hist.optJSONObject(i).optBoolean("ok"))ok++;a.fact(c,"本机最近抽检",ok+" / "+hist.length()+" 次通过（最多 20 次）");}}
  if(s!=null&&sub==200&&s.optInt("routing_version")==2)a.fact(c,"分流规则","v2 · 国内直连 / 国外代理 · MRS 每日更新");
  a.fact(c,"自动发布",cfg.optBoolean("auto_latest")?"已开启":"未开启");a.fact(c,"本机最近发布结果",cfg.optString("note","尚无本机发布记录"));
  c.addView(a.label("检查来自本机网络；连接失败不能判定 Cloudflare 故障。跟随首页自动更新，每 5 分钟抽检；历史通过次数不代表全天可用率。",12,a.MUTED));
  a.button(c,a.loading("cloud")?"正在检查 Cloudflare…":"立即检查 Cloudflare",()->a.load("cloud",()->check(a.manager),refreshed));a.button(c,"修改固定入口设置",()->new CloudScreen(a).edit());a.row(c,"诊断日志","查看 / 复制最近 100 轮",()->new CloudLogScreen(a).open());
 }
}
