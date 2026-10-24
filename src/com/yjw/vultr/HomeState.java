package com.yjw.vultr;
public final class HomeState {
 public static boolean fresh(long checked,long now){return checked>0&&now>=checked&&now-checked<90000;}
 public static boolean running(String status,String power){return "active".equals(status)&&"running".equals(power);}
 public static String state(String status,String power,boolean fresh){
  if(!fresh)return "等待更新";
  if(running(status,power))return "运行中";
  if("stopped".equals(power))return "已关机";
  if("pending".equals(status)||"installing".equals(status))return "部署中";
  return "待核对";
 }
}
