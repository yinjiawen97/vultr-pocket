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
 public static boolean inactive(String phase){return phase.equals("deleted")||phase.endsWith("rejected");}
}
