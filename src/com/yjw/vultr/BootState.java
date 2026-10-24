package com.yjw.vultr;
import org.json.JSONObject;
import java.time.Instant;
// Cloud observations only. Installingbooting does not expose firmware/OS progress.
public final class BootState {
 public static final long OBSERVE_MS=180000,INTERVAL_MS=10000;
 private BootState(){}
 public static String kind(String status,String power,String server,boolean fresh){
  if(!fresh)return "unknown";
  if("pending".equals(status))return "creating";
  if("stopped".equals(power))return "installingbooting".equals(server)?"creating":"stopped";
  if("installing".equals(status)||"installingbooting".equals(server))return "booting";
  if(HomeState.running(status,power)&&("ok".equals(server)||server==null||server.isEmpty()))return "running";
  return "unknown";
 }
 public static String label(String kind){switch(kind){case "creating":return "正在创建 · 等待开机";case "booting":return "正在开机 / 初始化";case "running":return "运行中";case "stopped":return "已关机";case "deleted":return "实例已失效";case "uncertain":return "请求结果待核对";default:return "等待更新";}}
 public static boolean starting(String kind){return "creating".equals(kind)||"booting".equals(kind);}
 public static long checked(JSONObject r){try{return Instant.parse(r.optString("checked_at")).toEpochMilli();}catch(Exception e){return 0;}}
 public static String field(JSONObject r,String name){if(r.has(name))return r.optString(name);String[] old=r.optString("cloud").split(" / ",-1);return name.equals("status")&&old.length==2?old[0]:name.equals("power_status")&&old.length==2?old[1]:"";}
 public static String record(JSONObject r,long now,boolean failed){if(Core.inactive(r.optString("phase")))return "deleted";if(Core.uncertain(r.optString("phase")))return "uncertain";return kind(field(r,"status"),field(r,"power_status"),r.optString("server_status"),!failed&&HomeState.fresh(checked(r),now));}
 public static boolean polling(String kind,String phase,boolean hasId,long elapsed,boolean foreground){return foreground&&elapsed>=0&&elapsed<OBSERVE_MS&&observe(kind,phase,hasId);}
 public static boolean observe(String kind,String phase,boolean hasId){return hasId&&!Core.inactive(phase)&&!Core.uncertain(phase)&&(starting(kind)||"unknown".equals(kind)&&"created".equals(phase));}
}
