package com.yjw.vultr;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public final class Core {
 public static BigDecimal money(Object value) { if(value==null)throw new IllegalArgumentException("缺少金额"); return new BigDecimal(value.toString()); }
 public static boolean budget(Object price,String cap) { BigDecimal c=money(cap),p=money(price); if(c.signum()<=0||p.signum()<0)throw new IllegalArgumentException("金额必须有效且上限大于零");return p.compareTo(c)<=0; }
 public static String cash(BigDecimal v){return "$"+v.setScale(2,java.math.RoundingMode.HALF_UP).toPlainString();}
 public static boolean overlaps(String end,String created){try{return OffsetDateTime.parse(end).isAfter(OffsetDateTime.parse(created));}catch(Exception e){return false;}}
 public static String time(String raw,java.time.ZoneId zone){
  if(raw==null||raw.isEmpty())return "尚无记录";
  try{return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX").format(OffsetDateTime.parse(raw).atZoneSameInstant(zone));}catch(Exception e){return "时间格式未知";}
 }
 public static String time(String raw){return time(raw,java.time.ZoneId.systemDefault());}
 public static String shortTime(String raw,java.time.ZoneId zone,java.time.Instant now){
  if(raw==null||raw.isEmpty())return "尚无记录";
  try{java.time.ZonedDateTime t=OffsetDateTime.parse(raw).atZoneSameInstant(zone),n=now.atZone(zone);String f=t.toLocalDate().equals(n.toLocalDate())?"HH:mm":t.getYear()==n.getYear()?"MM-dd HH:mm":"yyyy-MM-dd";return java.time.format.DateTimeFormatter.ofPattern(f).format(t);}catch(Exception e){return "时间格式未知";}
 }
 public static String shortTime(String raw){return shortTime(raw,java.time.ZoneId.systemDefault(),java.time.Instant.now());}
 public static String shortTime(long epochMillis){return shortTime(java.time.Instant.ofEpochMilli(epochMillis).toString());}
 public static boolean confirm(String input,String word){return input.trim().equalsIgnoreCase(word);}
 public static boolean uncertain(String phase){return phase.endsWith("pending")||phase.equals("reinstall-preparing");}
 // Releases whose archive ships every package assets/bootstrap.sh installs; reinstall keeps the instance's current OS.
 public static final String[] SYSTEMS={"Ubuntu 24.04 LTS x64","Ubuntu 26.04 LTS x64","Debian 12 x64 (bookworm)","Debian 13 x64 (trixie)"};
 public static boolean deployable(String os){for(String s:SYSTEMS)if(s.equals(os))return true;return false;}
 // Saved default system; anything unknown falls back to the first supported release.
 public static String system(String saved){return deployable(saved)?saved:SYSTEMS[0];}
 // Why "reinstall and deploy" is unavailable for an instance running this OS; empty when it is available.
 public static String blocked(String os){if(deployable(os))return "";return os==null||os.trim().isEmpty()?"尚未获取系统信息，刷新服务器列表后再试。":"当前系统 "+os.trim()+" 不支持自动部署，仅支持 Ubuntu 24.04 / 26.04 LTS 和 Debian 12 / 13（x64）。";}
 // 4xx answers that mean the cloud refused the request outright, so nothing was started.
 public static boolean refused(int code){return code==400||code==401||code==403||code==404||code==422;}
 public static boolean inactive(String phase){return phase.equals("deleted")||phase.endsWith("rejected");}
}
