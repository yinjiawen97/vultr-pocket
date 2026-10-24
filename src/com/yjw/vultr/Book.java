package com.yjw.vultr;
import org.json.*;
import java.util.*;
import java.io.*;
import java.net.*;
final class Book {
 final Manager m;Book(Manager m){this.m=m;}
 JSONArray all()throws Exception{if(!m.data.has("favorites"))m.data.put("favorites",new JSONArray());return m.data.getJSONArray("favorites");}
 JSONObject find(String key)throws Exception{JSONArray a=all();for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("key").equals(key))return a.getJSONObject(i);return null;}
 static String url(JSONObject r){String url=r.optString("url"),ip=r.optString("ip"),token=r.optString("token");if(!url.isEmpty())return url;if(Core.inactive(r.optString("phase"))||token.isEmpty()||!ip.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")||ip.equals("0.0.0.0"))return "";return "http://"+ip+"/sub/"+token;}
 JSONObject latest(String id)throws Exception{for(int i=m.records().length()-1;i>=0;i--){JSONObject r=m.records().getJSONObject(i);if(!id.isEmpty()&&id.equals(r.optString("id")))return r;}return null;}
 boolean deleted(JSONObject f){String id=f.optString("instance");JSONArray snapshot=m.data.optJSONArray("home_snapshot");if(!id.isEmpty()&&snapshot!=null&&HomeState.fresh(m.data.optLong("home_checked"),System.currentTimeMillis()))for(int i=0;i<snapshot.length();i++){JSONObject x=snapshot.optJSONObject(i);if(x!=null&&id.equals(x.optString("id"))){f.remove("instance_deleted");return false;}}return f.optBoolean("instance_deleted");}
 String status(JSONObject f){if(deleted(f))return "服务器已删除";String status=f.optString("status","尚未检测");return status.contains("已删除")?"状态待核对":status;}
 JSONObject importRecord(JSONObject f)throws Exception{String url=f.getString("url");if(f.optString("source").startsWith("record:")){JSONObject r=latest(f.optString("instance"));if(r!=null&&!url(r).isEmpty())url=url(r);}return new JSONObject().put("url",url).put("label",f.optString("name","订阅")).put("phase",deleted(f)?"deleted":"favorite");}
 void sync()throws Exception{synchronized(m){syncLocked();}}
 void syncLocked()throws Exception{
  JSONArray favorites=all();LinkedHashMap<String,JSONObject> newest=new LinkedHashMap<>();for(int i=0;i<m.records().length();i++){JSONObject r=m.records().getJSONObject(i);if(!r.optString("id").isEmpty())newest.put(r.getString("id"),r);}
  for(JSONObject r:newest.values()){
   String id=r.getString("id"),url=url(r);if(url.isEmpty())continue;String source="record:"+id+":"+r.optString("token");JSONObject chosen=null;
   for(int j=0;j<favorites.length();j++){JSONObject f=favorites.getJSONObject(j);if(source.equals(f.optString("source"))){chosen=f;break;}if(chosen==null&&id.equals(f.optString("instance"))&&f.optString("source").startsWith("record:"))chosen=f;}
   if(chosen==null){chosen=new JSONObject().put("key",UUID.randomUUID().toString()).put("name",r.optString("label","部署节点")).put("status","尚未检测");favorites.put(chosen);}
   boolean changed=!url.equals(chosen.optString("url"));chosen.put("url",url).put("source",source).put("instance",id).remove("superseded");
   if(r.optString("phase").equals("deleted"))chosen.put("instance_deleted",true).put("hidden",true);else if(!Core.inactive(r.optString("phase"))){chosen.remove("instance_deleted");if(changed||chosen.optString("status").contains("已删除")){chosen.put("status","尚未检测").put("failures",0);chosen.remove("report");chosen.remove("checked");}}
   for(int j=0;j<favorites.length();j++){JSONObject f=favorites.getJSONObject(j);if(f!=chosen&&id.equals(f.optString("instance"))&&f.optString("source").startsWith("record:")){f.put("superseded",true).put("status","历史部署订阅（已被新部署替代）");if(!Core.inactive(r.optString("phase")))f.remove("instance_deleted");}}
  }
  for(int j=0;j<favorites.length();j++){JSONObject f=favorites.getJSONObject(j);if(f.optString("source").startsWith("record:")&&deleted(f))f.put("hidden",true);}
  m.save();
 }
 void edit(JSONObject item,String name,String url)throws Exception{String normalized=SubscriptionLinks.valid(url.trim());if(name.trim().isEmpty())throw new IOException("请填写名称");for(int i=0;i<all().length();i++){JSONObject other=all().getJSONObject(i);if(other!=item&&!other.optBoolean("hidden")&&normalized.equals(other.optString("url")))throw new IOException("此订阅已经收藏，请编辑已有条目");}if(item==null){item=new JSONObject().put("key",UUID.randomUUID().toString());all().put(item);}else if(!normalized.equals(item.optString("url"))){item.remove("instance");item.remove("source");item.remove("instance_deleted");item.remove("superseded");item.remove("report");item.remove("checked");item.put("failures",0);}item.put("name",name.trim()).put("url",normalized).put("hidden",false).put("status","尚未检测");m.save();}
 static String download(String url)throws Exception{
  String current=SubscriptionLinks.valid(url);for(int i=0;i<4;i++){HttpURLConnection c=(HttpURLConnection)new URL(current).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);try{int code=c.getResponseCode();if(code>=300&&code<400){String location=c.getHeaderField("Location");if(location==null)throw new IOException("订阅重定向缺少地址");String next=new URL(new URL(current),location).toString();SubscriptionLinks.valid(next);if(current.startsWith("https:")&&!next.startsWith("https:"))throw new IOException("不接受从 HTTPS 降级的订阅重定向");current=next;continue;}if(code!=200)throw new IOException("订阅 HTTP "+code);return Api.read(c.getInputStream(),2*1024*1024);}finally{c.disconnect();}}throw new IOException("订阅重定向过多");
 }
 String check(JSONObject f,boolean deep)throws Exception{
  StringBuilder report=new StringBuilder();boolean bad=false;
  if(!f.optString("instance").isEmpty()&&!m.data.optString("key").isEmpty())try{m.api().call("GET","/instances/"+UUID.fromString(f.getString("instance")),null);f.remove("instance_deleted");report.append("云端实例：存在\n");}catch(Api.Error e){if(e.code==404){f.put("instance_deleted",true).put("status","服务器已删除").put("report","Vultr 已确认实例不存在。订阅可能已失效。").put("checked",Manager.now());m.save();return f.getString("report");}report.append("云端状态：暂时无法确认\n");}catch(Exception e){report.append("云端状态：暂时无法确认\n");}
  Map<String,Object> config;try{String raw=download(f.getString("url"));config=Configs.parse(raw);List<Map<String,Object>> nodes=Configs.nodes(config);f.put("node_count",nodes.size()).put("failures",0);report.append("订阅下载：成功 · ").append(nodes.size()).append(" 个节点\n");}
  catch(Exception e){int fails=f.optInt("failures")+1;f.put("failures",fails);config=null;if(deep)for(int i=m.records().length()-1;i>=0;i--){JSONObject r=m.records().getJSONObject(i);if(f.optString("url").equals(r.optString("url"))&&r.has("password")&&r.has("ip")&&!Core.inactive(r.optString("phase"))){config=Configs.parse(m.config(r,r.getString("ip")));break;}}report.append("订阅下载/格式：").append(safeMessage(e)).append('\n');if(config==null){f.put("status",fails>=2?"订阅连续失败，待复查":"订阅异常，待复查").put("report",report+"端口连接：未测试\n代理出口：未测试").put("checked",Manager.now());m.save();return f.getString("report");}bad=true;report.append("继续使用本机已保存的节点参数检测；不代表订阅已恢复。\n");}
  if(deep){List<Map<String,Object>> nodes=Configs.nodes(config);for(Map<String,Object> n:nodes){if(NodeProbe.cancelled){bad=true;report.append("剩余检测已取消\n");break;}report.append("\n节点：").append(String.valueOf(n.getOrDefault("name","未命名"))).append('\n');boolean port=false;long start=System.nanoTime();try(java.net.Socket s=new java.net.Socket()){s.connect(new InetSocketAddress(String.valueOf(n.get("server")),((Number)n.get("port")).intValue()),5000);port=true;report.append("端口连接：成功（").append((System.nanoTime()-start)/1000000).append(" ms）\n");}catch(Exception e){bad=true;report.append("端口连接：失败，网络或服务待核对\n");}if(port){try{String exit=NodeProbe.exit(m.context,n);String expected=f.optString("instance").isEmpty()?"":new URI(f.getString("url")).getHost();report.append("代理出口：成功 · ").append(exit);if(expected!=null&&!expected.isEmpty()&&!exit.equals(expected))report.append("（与订阅主机不同，需核对）");report.append('\n');}catch(Exception e){bad=true;report.append("代理出口：未通过 · ").append(safeMessage(e)).append('\n');}}else report.append("代理出口：未测试\n");}
  }else report.append("端口连接：未测试\n代理出口：未测试（点击完整节点检测）\n");
  f.put("status",deep?(bad?"部分检测未通过":"节点检测通过"):"订阅可下载").put("report",report.toString()).put("checked",Manager.now());m.save();return report.toString();
 }
 static String safeMessage(Exception e){String s=e.getMessage();if(e instanceof IOException&&s!=null&&!s.contains("://")&&!s.contains("password")&&s.length()<100)return s;return "连接或格式不符合要求，请稍后复查";}
}
