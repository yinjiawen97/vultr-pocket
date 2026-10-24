package com.yjw.vultr;
import org.json.*;
import java.net.*;
import java.io.*;
import javax.net.ssl.SSLException;
import java.time.Instant;
// Allowlisted diagnostics, never exception messages, stack traces, URLs or response bodies.
public final class CloudDiagnostics {
 public static final int LIMIT=100;
 private CloudDiagnostics(){}
 static String pick(String value,String allowed,String fallback){for(String s:allowed.split(","))if(s.equals(value))return s;return fallback;}
 public static String host(String url){try{URI u=new URI(url);String h=u.getHost();return "https".equals(u.getScheme())&&u.getUserInfo()==null&&h!=null&&h.length()<=253&&h.matches("[A-Za-z0-9.\\-:\\[\\]]+")?h:"地址无效";}catch(Exception e){return "地址无效";}}
 public static String reason(Throwable e){if(e==null)return "ok";for(int i=0;e!=null&&i<8;i++,e=e.getCause()){
  if(e instanceof UnknownHostException)return "dns";
  if(e instanceof SocketTimeoutException)return "timeout";
  if(e instanceof SSLException||e instanceof java.security.cert.CertificateException)return "tls";
  if(e instanceof NoRouteToHostException)return "route";
  if(e instanceof ConnectException)return "connect";
  if(e instanceof SocketException)return "socket";
  if(e instanceof JSONException||e instanceof IllegalArgumentException)return "format";
  if(e instanceof URISyntaxException||e instanceof MalformedURLException)return "url";
  if(e instanceof SecurityException)return "restricted";
 }return "io";}
 public static String label(String reason){switch(reason){case "ok":return "通过";case "dns":return "DNS 域名解析失败";case "timeout":return "连接或读取超时";case "tls":return "TLS / 证书验证失败";case "route":return "网络路由不可达";case "connect":return "无法建立连接";case "socket":return "连接中断";case "format":return "响应格式不符合要求";case "url":return "地址格式无效";case "restricted":return "网络访问受限制";case "http":return "HTTP 响应未通过";default:return "输入输出失败（原因未细分）";}}
 static String stage(String x){switch(x){case "health":return "Worker 响应";case "admin":return "管理权限";default:return "订阅下载 / 格式";}}
 static String phase(String x){switch(x){case "setup":return "准备请求";case "response":return "建立连接 / 等待响应头";case "body":return "读取响应";default:return "校验内容";}}
 static String source(String x){switch(x){case "home":return "首页刷新";case "background":return "后台抽检";case "subscription":return "订阅页检测";case "publish":return "发布后核验";case "settings":return "设置验证后检测";default:return "手动检测";}}
 public static JSONObject event(String stage,String url,String phase,int status,long elapsed,Throwable error)throws JSONException{
  String reason=error==null?"ok":status>0&&status!=200?"http":reason(error);
  return cleanEvent(new JSONObject().put("stage",stage).put("host",host(url)).put("phase",phase).put("http",status).put("elapsed_ms",elapsed).put("reason",reason));
 }
 static JSONObject cleanEvent(JSONObject x)throws JSONException{
  String h=x.optString("host");if(!h.matches("[A-Za-z0-9.\\-:\\[\\]]{1,253}"))h="地址无效";int status=x.optInt("http");
  return new JSONObject().put("stage",pick(x.optString("stage"),"health,admin,subscription","subscription")).put("host",h).put("phase",pick(x.optString("phase"),"setup,response,body,format","setup")).put("http",status>=100&&status<=599?status:0).put("elapsed_ms",Math.max(0,x.optLong("elapsed_ms"))).put("reason",pick(x.optString("reason"),"ok,dns,timeout,tls,route,connect,socket,format,url,restricted,http,io","io"));
 }
 static JSONObject cleanRound(JSONObject x)throws JSONException{
  JSONArray checks=new JSONArray(),raw=x.optJSONArray("checks");boolean ok=true;if(raw!=null)for(int i=0;i<Math.min(3,raw.length());i++){JSONObject item=raw.optJSONObject(i);if(item==null)continue;JSONObject safe=cleanEvent(item);checks.put(safe);ok&=safe.optString("reason").equals("ok");}
  String version=x.optString("version","unknown");if(!version.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}"))version="unknown";
  return new JSONObject().put("checked",Math.max(0,x.optLong("checked"))).put("source",pick(x.optString("source"),"manual,home,background,subscription,publish,settings","manual")).put("network",pick(x.optString("network"),"wifi,cellular,ethernet,other,none,unknown","unknown")).put("vpn",x.optBoolean("vpn")).put("validated",x.optBoolean("validated")).put("elapsed_ms",Math.max(0,x.optLong("elapsed_ms"))).put("checks",checks).put("ok",checks.length()==3&&ok).put("version",version);
 }
 public static JSONObject append(JSONObject old,JSONObject round)throws JSONException{
  JSONObject safe=cleanRound(round),out=new JSONObject();JSONArray raw=old==null?null:old.optJSONArray("rounds"),next=new JSONArray();if(raw!=null)for(int i=Math.max(0,raw.length()-(LIMIT-1));i<raw.length();i++){JSONObject item=raw.optJSONObject(i);if(item!=null)next.put(cleanRound(item));}next.put(safe);out.put("rounds",next);
  JSONObject failure=!safe.optBoolean("ok")?safe:old==null?null:old.optJSONObject("last_failure");if(failure!=null)out.put("last_failure",cleanRound(failure));return out;
 }
 public static String roundText(JSONObject input){try{JSONObject x=cleanRound(input);StringBuilder s=new StringBuilder(Instant.ofEpochMilli(x.optLong("checked")).toString()).append(" · ").append(x.optBoolean("ok")?"通过":"未通过");String n=x.optString("network");s.append("\n").append(source(x.optString("source"))).append(" · ").append(n.equals("wifi")?"Wi-Fi":n.equals("cellular")?"移动网络":n.equals("ethernet")?"以太网":n.equals("none")?"无活动网络":n.equals("other")?"其他网络":"网络类型未知").append(" · VPN ").append(x.optBoolean("vpn")?"存在":"未观测到").append(" · 系统联网验证 ").append(x.optBoolean("validated")?"通过":"未确认").append("\n整轮 ").append(x.optLong("elapsed_ms")).append(" ms · App ").append(x.optString("version"));JSONArray checks=x.getJSONArray("checks");for(int i=0;i<checks.length();i++){JSONObject e=checks.getJSONObject(i);s.append("\n\n").append(stage(e.optString("stage"))).append("：").append(label(e.optString("reason"))).append("\n域名：").append(e.optString("host")).append("\n阶段：").append(phase(e.optString("phase"))).append(" · ").append(e.optLong("elapsed_ms")).append(" ms · HTTP ").append(e.optInt("http")==0?"未收到":e.optInt("http"));}return s.toString();}catch(Exception e){return "日志记录不完整";}}
 public static String report(JSONObject data){StringBuilder s=new StringBuilder("Cloudflare 本机诊断日志（时间为 UTC）\n不含密钥、订阅路径、配置内容或原始异常。系统联网验证不代表 Cloudflare 可达；VPN 存在不代表本次请求实际经由 VPN。\n");if(data==null)return s.append("\n尚无日志；旧抽检无法补回详细原因。").toString();JSONObject failed=data.optJSONObject("last_failure");if(failed!=null)s.append("\n最近一次失败\n").append(roundText(failed)).append('\n');JSONArray rounds=data.optJSONArray("rounds");if(rounds!=null)for(int i=rounds.length()-1;i>=Math.max(0,rounds.length()-LIMIT);i--){JSONObject x=rounds.optJSONObject(i);if(x!=null)s.append("\n----------------\n").append(roundText(x)).append('\n');}return s.toString();}
}
