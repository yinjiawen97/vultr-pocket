package com.yjw.vultr;
import java.math.BigDecimal;
import java.util.*;
import org.json.*;
final class Inventory {
 static BigDecimal price(JSONObject plan,String region)throws Exception{
  JSONObject costs=plan.optJSONObject("location_cost");JSONObject local=costs==null?null:costs.optJSONObject(region);
  // An advertised regional price takes precedence, including zero; malformed values must fail closed.
  return Core.money(local!=null&&local.has("monthly_cost")?local.get("monthly_cost"):plan.get("monthly_cost"));
 }
 static boolean located(JSONObject plan,String region)throws Exception{JSONArray a=plan.optJSONArray("locations");if(a==null)return false;for(int i=0;i<a.length();i++)if(a.getString(i).equals(region))return true;return false;}
 static boolean supported(JSONObject p){return Arrays.asList("vc2","vhf","vhp","voc","vx1").contains(p.optString("type"))&&p.optBoolean("deploy_ondemand",true)&&p.optInt("ram")>=1024&&!p.optString("id").endsWith("-v6")&&!p.optString("cpu_vendor").toLowerCase(Locale.ROOT).contains("arm");}
 static Set<String> available(JSONObject response)throws Exception{Set<String> ids=new HashSet<>();JSONArray a=response.getJSONArray("available_plans");for(int i=0;i<a.length();i++)ids.add(a.getString(i));return ids;}
 static String specs(JSONObject p){return p.optInt("vcpu_count")+" vCPU · "+new BigDecimal(p.optInt("ram")).divide(new BigDecimal(1024)).stripTrailingZeros().toPlainString()+" GB 内存 · "+p.optInt("disk")+" GB "+p.optString("disk_type")+"\n流量 "+p.optInt("bandwidth")+" GB/月 · "+p.optString("cpu_vendor");}
}
