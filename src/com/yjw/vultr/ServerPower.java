package com.yjw.vultr;
import org.json.*;
import java.io.IOException;
import java.util.*;
public final class ServerPower {
 public static final long WINDOW=180000,INTERVAL=10000;
 public interface Transport {JSONObject call(String method,String path,JSONObject body)throws Exception;}
 public interface HttpFailure {int status();}
 public interface Pending {void begin(String id,String action)throws Exception;void clear(String id)throws Exception;}
 public static final class Result {public final JSONObject instance;public final boolean sent,confirmed,uncertain;Result(JSONObject x,boolean s,boolean c,boolean u)throws Exception{instance=safe(x);sent=s;confirmed=c;uncertain=u;}}
 private ServerPower(){}
 public static String id(String value){if(value==null||!value.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"))throw new IllegalArgumentException("实例 ID 无效");return UUID.fromString(value).toString();}
 public static String action(String value){if(!"start".equals(value)&&!"halt".equals(value))throw new IllegalArgumentException("不支持的电源操作");return value;}
 public static JSONObject safe(JSONObject raw)throws Exception{JSONObject x=new JSONObject();for(String k:new String[]{"id","label","main_ip","region","plan","os","status","power_status","server_status","date_created","vcpu_count","ram","disk","user_scheme","username"})if(raw.has(k)&&!raw.isNull(k))x.put(k,raw.get(k));return x;}
 public static boolean reached(JSONObject x,String action){return "active".equals(x.optString("status"))&&("start".equals(action)?"running":"stopped").equals(x.optString("power_status"))&&(!"start".equals(action)||"ok".equals(x.optString("server_status")));}
 public static boolean allowed(JSONObject x,String action){action(action);return "active".equals(x.optString("status"))&&"ok".equals(x.optString("server_status"))&&("start".equals(action)?"stopped":"running").equals(x.optString("power_status"));}
 public static boolean pending(JSONObject marker,long now){long t=marker==null?0:marker.optLong("sent");return t>0&&now>=t&&now-t<WINDOW;}
 public static JSONObject read(Transport api,String target)throws Exception{String exact=id(target);JSONObject x=api.call("GET","/instances/"+exact,null).getJSONObject("instance");if(!exact.equals(id(x.optString("id"))))throw new IOException("返回实例与目标不一致，已停止操作");return x;}
 public static Result execute(Transport api,String target,String action,Pending pending)throws Exception{
  String exact=id(target);action(action);JSONObject before=read(api,exact);
  if(reached(before,action)){pending.clear(exact);return new Result(before,false,true,false);}
  if(!allowed(before,action))throw new IOException("云端状态已变化或正在处理，请刷新后重新确认");
  pending.begin(exact,action); // Persist before the only POST. Never automatically replay it.
  try{api.call("POST","/instances/"+exact+"/"+action,null);}catch(Exception e){int status=e instanceof HttpFailure?((HttpFailure)e).status():0;if(status>=400&&status<500&&status!=408){pending.clear(exact);throw e;}return new Result(before,true,false,true);}
  try{JSONObject after=read(api,exact);boolean done=reached(after,action);if(done)pending.clear(exact);return new Result(after,true,done,false);}catch(Exception e){return new Result(before,true,false,true);}
 }
 public static void reconcile(JSONObject markers,JSONArray instances){if(markers==null)return;List<String> remove=new ArrayList<>();for(Iterator<String> it=markers.keys();it.hasNext();){String key=it.next();JSONObject mark=markers.optJSONObject(key),found=null;for(int i=0;i<instances.length();i++){JSONObject x=instances.optJSONObject(i);if(x!=null&&key.equals(x.optString("id"))){found=x;break;}}if(found==null||mark==null||reached(found,mark.optString("action")))remove.add(key);}for(String key:remove)markers.remove(key);}
}
