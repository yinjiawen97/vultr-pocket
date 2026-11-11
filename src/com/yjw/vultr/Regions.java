package com.yjw.vultr;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
// Display data for Vultr's region codes: Chinese city and country, flag and time zone. Unknown codes keep their code and show no time.
public final class Regions {
 private Regions(){}
 private static final Map<String,String[]> ALL=new HashMap<>();
 private static void put(String id,String city,String code,String country,String zone){ALL.put(id,new String[]{city,code,country,zone});}
 static{
  put("ams","阿姆斯特丹","NL","荷兰","Europe/Amsterdam");put("atl","亚特兰大","US","美国","America/New_York");put("blr","班加罗尔","IN","印度","Asia/Kolkata");
  put("bom","孟买","IN","印度","Asia/Kolkata");put("cdg","巴黎","FR","法国","Europe/Paris");put("del","德里","IN","印度","Asia/Kolkata");
  put("dfw","达拉斯","US","美国","America/Chicago");put("ewr","新泽西","US","美国","America/New_York");put("fra","法兰克福","DE","德国","Europe/Berlin");
  put("hnl","檀香山","US","美国","Pacific/Honolulu");put("icn","首尔","KR","韩国","Asia/Seoul");put("itm","大阪","JP","日本","Asia/Tokyo");
  put("jnb","约翰内斯堡","ZA","南非","Africa/Johannesburg");put("lax","洛杉矶","US","美国","America/Los_Angeles");put("lhr","伦敦","GB","英国","Europe/London");
  put("mad","马德里","ES","西班牙","Europe/Madrid");put("man","曼彻斯特","GB","英国","Europe/London");put("mel","墨尔本","AU","澳大利亚","Australia/Melbourne");
  put("mex","墨西哥城","MX","墨西哥","America/Mexico_City");put("mia","迈阿密","US","美国","America/New_York");put("mxp","米兰","IT","意大利","Europe/Rome");
  put("nrt","东京","JP","日本","Asia/Tokyo");put("ord","芝加哥","US","美国","America/Chicago");put("sao","圣保罗","BR","巴西","America/Sao_Paulo");
  put("scl","圣地亚哥","CL","智利","America/Santiago");put("sea","西雅图","US","美国","America/Los_Angeles");put("sgp","新加坡","SG","新加坡","Asia/Singapore");
  put("sjc","硅谷","US","美国","America/Los_Angeles");put("sto","斯德哥尔摩","SE","瑞典","Europe/Stockholm");put("syd","悉尼","AU","澳大利亚","Australia/Sydney");
  put("tlv","特拉维夫","IL","以色列","Asia/Jerusalem");put("waw","华沙","PL","波兰","Europe/Warsaw");put("yto","多伦多","CA","加拿大","America/Toronto");
 }
 private static String[] row(String id){return id==null?null:ALL.get(id.trim().toLowerCase(Locale.ROOT));}
 public static String code(String id){return id==null?"":id.trim().toUpperCase(Locale.ROOT);}
 // "硅谷 · 美国"; a city state is named once; an unknown code is shown as is.
 public static String title(String id){String[] r=row(id);if(r==null)return code(id).isEmpty()?"地区待获取":code(id);return r[0].equals(r[2])?r[0]:r[0]+" · "+r[2];}
 // Flag emoji built from the ISO country code; a globe when the region is unknown.
 public static String flag(String id){String[] r=row(id);if(r==null)return "🌐";StringBuilder b=new StringBuilder();for(char c:r[1].toCharArray())b.appendCodePoint(0x1F1E6+(c-'A'));return b.toString();}
 // Time zone id, or null when the region is unknown.
 public static String zone(String id){String[] r=row(id);return r==null?null:r[3];}
 // How the region's clock compares with the phone's right now, e.g. "比本机慢 16 小时".
 public static String offset(String id,ZoneId device,Instant now){
  String zone=zone(id);if(zone==null)return "";
  int minutes=(ZoneId.of(zone).getRules().getOffset(now).getTotalSeconds()-device.getRules().getOffset(now).getTotalSeconds())/60;
  if(minutes==0)return "与本机同一时间";
  int size=Math.abs(minutes),hours=size/60,rest=size%60;
  return "比本机"+(minutes>0?"快":"慢")+" "+(hours>0?hours+" 小时":"")+(hours>0&&rest>0?" ":"")+(rest>0?rest+" 分钟":"");
 }
}
