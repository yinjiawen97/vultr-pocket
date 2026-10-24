package com.yjw.vultr;
import org.json.JSONObject;
public final class ServerLogin {
 private ServerLogin(){}
 public static String username(JSONObject raw){String supplied=raw.isNull("username")?"":raw.optString("username").trim();if(!supplied.isEmpty())return supplied;if(!linux(raw))return "";String scheme=raw.optString("user_scheme");if("root".equals(scheme))return "root";if("limited".equals(scheme))return "linuxuser";return "";}
 public static String password(JSONObject raw){return raw.isNull("default_password")?"":raw.optString("default_password","");} // Never use the Shadowsocks "password" field.
 public static String host(JSONObject raw){return raw.isNull("main_ip")?raw.optString("ip"):raw.optString("main_ip");}
 public static boolean linux(JSONObject raw){String f=ServerIdentity.family(raw.optString("os"));return !f.equals("windows")&&!f.equals("macos")&&!f.equals("unknown");}
 public static String command(JSONObject raw){String u=username(raw),h=host(raw);if(!linux(raw)||!u.matches("[A-Za-z_][A-Za-z0-9_-]{0,31}")||!h.matches("[0-9A-Fa-f:.]+"))return "";return "ssh -p 22 "+u+"@"+h;}
 public static JSONObject cached(JSONObject data,String id){JSONObject all=data.optJSONObject("server_logins");return all==null?null:all.optJSONObject(id);}
 public static boolean fresh(JSONObject entry,long now){if(entry!=null&&(entry.optBoolean("manual")||entry.optBoolean("initialized")))return true;long t=entry==null?0:entry.optLong("checked");return t>0&&now>=t&&now-t<300000&&(entry.optBoolean("detail")||!password(entry).isEmpty());}
 // Separate encrypted cache; the public snapshot and diagnostics still exclude passwords.
 public static void remember(JSONObject data,JSONObject raw,long now,boolean detail)throws Exception{
  if(!detail&&password(raw).isEmpty())return;
  String id;try{id=ServerPower.id(raw.optString("id"));}catch(IllegalArgumentException e){return;}
  JSONObject previous=cached(data,id);if(previous!=null&&(previous.optBoolean("manual")||previous.optBoolean("initialized")))return;
  JSONObject entry=new JSONObject().put("id",id).put("checked",now).put("detail",detail);
  for(String k:new String[]{"os","username","user_scheme","main_ip","default_password"})if(raw.has(k)&&!raw.isNull(k)&&raw.opt(k) instanceof String)entry.put(k,raw.getString(k));
  if(password(entry).isEmpty()&&previous!=null&&!password(previous).isEmpty())entry.put("default_password",password(previous));
  JSONObject all=data.optJSONObject("server_logins");if(all==null){all=new JSONObject();data.put("server_logins",all);}all.put(id,entry);
 }
 // SSH account created by the deploy script: username from settings (default "vultr"), a fresh random password per deployment.
 public static final String DEFAULT_USER="vultr";
 public static boolean validUser(String u){return u!=null&&u.matches("[a-z_][a-z0-9_-]{0,31}")&&!u.equals("root");}
 public static String deployUser(String configured){return validUser(configured)?configured:DEFAULT_USER;}
 public static String newPassword(){String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";java.security.SecureRandom r=new java.security.SecureRandom();StringBuilder b=new StringBuilder();for(int i=0;i<20;i++)b.append(alphabet.charAt(r.nextInt(alphabet.length())));return b.toString();}
 public static void initialized(JSONObject data,JSONObject record,long now)throws Exception{
  if(!"subscription-ready".equals(record.optString("phase"))||!validUser(record.optString("ssh_username"))||record.optString("ssh_password").isEmpty())return;
  String id=ServerPower.id(record.optString("id"));JSONObject previous=cached(data,id);if(previous!=null&&(previous.optBoolean("manual")||previous.optBoolean("initialized")))return;
  manual(data,new JSONObject(record.toString()).put("main_ip",record.optString("ip")),record.getString("ssh_username"),record.getString("ssh_password"),now);
  cached(data,id).remove("manual");cached(data,id).put("initialized",true);
 }
 public static void forget(JSONObject data,String id){JSONObject all=data.optJSONObject("server_logins");if(all!=null)all.remove(id);}
 public static void manual(JSONObject data,JSONObject target,String username,String password,long now)throws Exception{String id=ServerPower.id(target.optString("id"));if(username==null||username.trim().isEmpty()||username.length()>128||username.contains("\n")||username.contains("\r"))throw new IllegalArgumentException("请输入有效用户名");JSONObject entry=ServerPower.safe(target).put("id",id).put("username",username.trim()).put("default_password",password==null?"":password).put("manual",true).put("checked",now);JSONObject all=data.optJSONObject("server_logins");if(all==null){all=new JSONObject();data.put("server_logins",all);}all.put(id,entry);}
}
