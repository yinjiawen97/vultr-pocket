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
 // Systems offered when picking one. Any release matching deployable() works; this is just the menu.
 public static final String[] SYSTEMS={"Ubuntu 26.10 x64","Ubuntu 26.04 LTS x64","Ubuntu 24.04 LTS x64","Ubuntu 22.04 LTS x64","Debian 13 x64 (trixie)","Debian 12 x64 (bookworm)"};
 public static final String DEFAULT_SYSTEM="Ubuntu 24.04 LTS x64";
 private static final java.util.regex.Pattern UBUNTU=java.util.regex.Pattern.compile("Ubuntu (\\d+)\\.(\\d+)(?: LTS)? x64"),DEBIAN=java.util.regex.Pattern.compile("Debian (\\d+) x64 \\([a-z]+\\)");
 // assets/bootstrap.sh needs apt, shadowsocks-libev and sshd_config.d: every x64 Ubuntu from 22.04 and Debian from 12, by Vultr's exact image names.
 public static boolean deployable(String os){if(os==null)return false;java.util.regex.Matcher u=UBUNTU.matcher(os);if(u.matches())return Integer.parseInt(u.group(1))*100+Integer.parseInt(u.group(2))>=2204;java.util.regex.Matcher d=DEBIAN.matcher(os);return d.matches()&&Integer.parseInt(d.group(1))>=12;}
 public static boolean listed(String os){for(String s:SYSTEMS)if(s.equals(os))return true;return false;}
 // Menu for redeploying an instance: its own release first when it is deployable but newer than this list.
 public static String[] choices(String current){if(!deployable(current)||listed(current))return SYSTEMS;String[] all=new String[SYSTEMS.length+1];all[0]=current;System.arraycopy(SYSTEMS,0,all,1,SYSTEMS.length);return all;}
 // Saved default system; anything unknown falls back to the first supported release.
 public static String system(String saved){return listed(saved)?saved:DEFAULT_SYSTEM;}
 // Why "reinstall and deploy" is unavailable for an instance; empty when it is available. Only an unknown OS blocks it:
 // an instance on any other system can still be redeployed by switching it to one of SYSTEMS.
 public static String blocked(String os){return os==null||os.trim().isEmpty()?"尚未获取系统信息，刷新服务器列表后再试。":"";}
 // Shown under the button when the current OS cannot be deployed on as-is; empty otherwise.
 public static String switchHint(String os){return os==null||os.trim().isEmpty()||deployable(os)?"":"当前系统 "+os.trim()+" 不能直接部署；点「重装并部署」可更换为 Ubuntu（22.04 起）或 Debian（12 起）后部署。";}
 // 4xx answers that mean the cloud refused the request outright, so nothing was started.
 public static boolean refused(int code){return code==400||code==401||code==403||code==404||code==422;}
 public static boolean inactive(String phase){return phase.equals("deleted")||phase.endsWith("rejected");}
}
