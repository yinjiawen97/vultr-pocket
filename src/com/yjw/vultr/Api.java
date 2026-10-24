package com.yjw.vultr;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
final class Api {
 final String key; Api(String key){this.key=key;}
 static class Error extends IOException implements ServerPower.HttpFailure { final int code; public int status(){return code;} Error(int c){super("Vultr HTTP "+c+(c==401||c==403?"：检查 Key 权限和手机出口 IP 白名单。":"：请刷新核对；不要重复提交修改。"));code=c;} }
 JSONObject call(String method,String path,JSONObject body)throws Exception{
  if(key.isEmpty()||key.contains("\n")||key.contains("\r"))throw new IOException("请在设置中输入有效 API Key。");
  if(!path.startsWith("/")||path.startsWith("//"))throw new IOException("API 路径无效");
  HttpURLConnection c=(HttpURLConnection)new URL("https://api.vultr.com/v2"+path).openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestMethod(method);c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json");
  try{
   if(body!=null){c.setDoOutput(true);try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));}}
   int code=c.getResponseCode();if(code<200||code>=300)throw new Error(code);
   String text=read(c.getInputStream(),8*1024*1024);return text.trim().isEmpty()?new JSONObject():new JSONObject(text);
  }finally{c.disconnect();}
 }
 JSONArray list(String path,String field)throws Exception{
  JSONArray all=new JSONArray();String cursor="";Set<String> seen=new HashSet<>();
  for(int n=0;n<100;n++){
   JSONObject page=call("GET",path+"?per_page=100"+(cursor.isEmpty()?"":"&cursor="+URLEncoder.encode(cursor,"UTF-8")),null);JSONArray a=page.getJSONArray(field);for(int i=0;i<a.length();i++)all.put(a.get(i));
   JSONObject meta=page.optJSONObject("meta"),links=meta==null?null:meta.optJSONObject("links");String next=links==null?"":links.optString("next","");if(next.isEmpty())return all;
   if(next.startsWith("/")||next.contains("://")){String query=new URI(next).getRawQuery();next="";if(query!=null)for(String pair:query.split("&"))if(pair.startsWith("cursor="))next=URLDecoder.decode(pair.substring(7),"UTF-8");}
   if(next.isEmpty()||!seen.add(next))throw new IOException("分页异常，费用结果不完整。");cursor=next;
  }throw new IOException("分页超过限制，结果不完整。");
 }
 static String read(InputStream in,int limit)throws IOException {try(InputStream stream=in;ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=stream.read(buf))!=-1){if(b.size()+n>limit)throw new IOException("响应过大");b.write(buf,0,n);}return b.toString("UTF-8");}}
 static String subscription(String url)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(10000);try{if(c.getResponseCode()!=200)throw new IOException("订阅尚未就绪");return read(c.getInputStream(),1024*1024);}finally{c.disconnect();}}
}
