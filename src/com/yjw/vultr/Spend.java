package com.yjw.vultr;
import java.math.BigDecimal;
import java.time.*;
import org.json.*;
// Per-server running time and real spend, summed from Vultr bill lines (invoiced + pending) that belong to the instance.
public final class Spend {
 // A compute line belongs to the instance when it names its IP (and label, if bracketed) and ends after the instance was created; an older server on the same IP never matches.
 public static boolean matches(JSONObject item,JSONObject instance){String ip=instance.optString("main_ip"),name=instance.optString("label"),d=item.optString("description");return item.optString("product").equals("Vultr Cloud Compute")&&!ip.isEmpty()&&d.startsWith(ip+" ")&&(!d.endsWith("]")||d.endsWith("["+name+"]"))&&Core.overlaps(item.optString("end_date"),instance.optString("date_created"));}
 public static BigDecimal total(JSONArray items,JSONObject instance){BigDecimal sum=BigDecimal.ZERO;for(int i=0;i<items.length();i++){JSONObject r=items.optJSONObject(i);if(r!=null&&matches(r,instance))sum=sum.add(Core.money(r.opt("total")));}return sum;}
 public static String uptime(String created,Instant now){try{Duration d=Duration.between(OffsetDateTime.parse(created).toInstant(),now);if(d.isNegative())return "刚创建";long m=d.toMinutes(),h=d.toHours(),days=d.toDays();if(h<1)return Math.max(1,m)+" 分钟";if(days<1)return h+" 小时 "+(m%60)+" 分";return days+" 天 "+(h%24)+" 小时";}catch(Exception e){return "时间未知";}}
}
