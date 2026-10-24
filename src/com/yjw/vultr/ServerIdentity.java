package com.yjw.vultr;
import java.util.Locale;
import java.util.regex.Pattern;
// Display-only identification from the cloud's OS name; never infer it from a label or plan.
public final class ServerIdentity {
 private ServerIdentity(){}
 static boolean word(String value,String expression){return Pattern.compile("(?:^|[^a-z0-9])(?:"+expression+")(?:$|[^a-z0-9])").matcher(value).find();}
 public static String name(String os){return os==null||os.trim().isEmpty()?"系统待获取":os.trim();}
 public static String family(String os){String s=os==null?"":os.toLowerCase(Locale.ROOT);
  if(word(s,"ubuntu"))return "ubuntu";
  if(word(s,"debian"))return "debian";
  if(word(s,"windows"))return "windows";
  if(word(s,"macos|mac os(?: x)?|os x|darwin"))return "macos";
  if(word(s,"fedora"))return "fedora";
  if(word(s,"rocky(?: linux)?"))return "rocky";
  if(word(s,"almalinux|alma linux"))return "alma";
  if(word(s,"arch(?: linux)?"))return "arch";
  if(word(s,"linux|centos|centos stream|rhel|red hat|opensuse|suse|alpine|oracle linux|flatcar"))return "linux";
  return "unknown";
 }
 public static String host(String plan){return "Vultr 云主机 · "+(plan==null||plan.trim().isEmpty()?"套餐待获取":plan.trim());}
}
