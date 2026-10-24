package com.yjw.vultr;
import java.net.*;
import java.io.*;
import org.json.*;
final class CloudPublisher {
 static JSONObject config(Manager m){return m.data.optJSONObject("cloud");}
 static JSONObject call(JSONObject cloud,String method,String path,JSONObject body)throws Exception{
  if(cloud==null)throw new IOException("请先配置固定订阅入口");String base=cloud.optString("base");URI u=new URI(base);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||!u.getRawPath().isEmpty()&& !u.getRawPath().equals("/"))throw new IOException("固定入口必须是 HTTPS 根地址");
  String token=cloud.optString("admin");if(token.isEmpty()||token.contains("\n")||token.contains("\r"))throw new IOException("请配置管理密钥");
  HttpURLConnection c=(HttpURLConnection)new URL(base.replaceAll("/+$","")+path).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestMethod(method);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Content-Type","application/json");
  try{if(body!=null){c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}}int code=c.getResponseCode();if(code!=200)throw new IOException("固定订阅服务返回 HTTP "+code+"，原已发布配置保留");return new JSONObject(Api.read(c.getInputStream(),16384));}finally{c.disconnect();}
 }
 static void publish(Manager m,JSONObject config)throws Exception{JSONObject cloud=config(m);JSONObject result=call(cloud,"PUT","/api/config",new JSONObject().put("config",config));cloud.put("published",result.optString("updated_at")).put("revision",result.optString("revision")).put("note","最近发布成功");m.save();}
 static void upgrade(Manager m)throws Exception{
  JSONObject cloud=config(m);if(cloud==null)throw new IOException("请先配置固定订阅入口");
  final String url=cloud.getString("url");java.util.Map<String,Object> original=Configs.parse(Book.download(url));
  java.util.Map<String,Object> next=Configs.routing(Configs.parse(m.asset("config.yaml")),original);
  RoutingUpdate.apply(new RoutingUpdate.Endpoint(){public java.util.Map<String,Object> read()throws Exception{return Configs.parse(Book.download(url));}public void write(java.util.Map<String,Object> c)throws Exception{call(cloud,"PUT","/api/config",new JSONObject().put("config",new JSONObject(c)));}},
   old->{if(!old.equals(original))throw new IOException("固定订阅已变化，请重新升级");m.data.put("routing_backup",new JSONObject().put("config",new JSONObject(old)).put("saved",Manager.now()));m.save();},
   c->{Configs.nodes(c);RoutingValidation.check(m.context,c);},next);
  cloud.put("routing_version",2).put("note","分流已升级；请在客户端刷新固定订阅");m.save();
 }
 static void restoreRouting(Manager m)throws Exception{
  JSONObject backup=m.data.optJSONObject("routing_backup"),cloud=config(m);if(backup==null||cloud==null)throw new IOException("没有本机分流升级备份");
  java.util.Map<String,Object> old=Configs.parse(backup.getJSONObject("config").toString());Configs.nodes(old);
  try{call(cloud,"PUT","/api/config",new JSONObject().put("config",new JSONObject(old)));}catch(Exception e){if(!old.equals(Configs.parse(Book.download(cloud.getString("url")))))throw new IOException("恢复未确认，备份仍保留，请重试");}
  if(!old.equals(Configs.parse(Book.download(cloud.getString("url")))))throw new IOException("恢复回读未通过，请稍后复查");cloud.put("note","已恢复升级前订阅；请刷新客户端");m.save();
 }
 static void latest(Manager m,JSONObject record)throws Exception{JSONObject cloud=config(m);if(cloud==null||!cloud.optBoolean("auto_latest"))return;try{publish(m,new JSONObject(Configs.routing(Configs.parse(m.asset("config.yaml")),Configs.parse(Book.download(record.getString("url"))))));record.put("publish_note","已更新固定订阅，客户端刷新即可");}catch(Exception e){cloud.put("note","自动发布失败："+Book.safeMessage(e));record.put("publish_note","节点已就绪，但固定订阅发布失败，请到收藏夹手动重试");}m.save();}
}
