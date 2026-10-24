package com.yjw.vultr;
import java.net.URI;
import java.net.URLEncoder;
public final class SubscriptionLinks {
 public static String valid(String url){try{URI u=new URI(url);if(!("http".equals(u.getScheme())||"https".equals(u.getScheme()))||u.getHost()==null||u.getUserInfo()!=null||url.contains("\n")||url.contains("\r"))throw new IllegalArgumentException();return url;}catch(Exception e){throw new IllegalArgumentException("订阅地址无效，请先检查订阅。");}}
 public static String[] clientPackages(){return new String[]{"com.follow.clash","com.fclash.app"};}
 public static String clientName(String pkg){if("com.follow.clash".equals(pkg))return "FlClash · GitHub 官方版";if("com.fclash.app".equals(pkg))return "FlClash · SumCheung 版";throw new IllegalArgumentException("不支持的导入客户端");}
 public static String[] clients(boolean official,boolean alternative){return official?(alternative?clientPackages():new String[]{"com.follow.clash"}):(alternative?new String[]{"com.fclash.app"}:new String[0]);}
 static String encode(String s){try{return URLEncoder.encode(s,"UTF-8").replace("+","%20");}catch(Exception e){throw new IllegalArgumentException("无法生成导入链接");}}
 public static String flclash(String url){return "flclash://install-config?url="+encode(valid(url));}
 // Verge's parser treats everything after url= as the URL; put name first.
 public static String verge(String url,String name){return "clash-verge://install-config?name="+encode(name)+"&url="+encode(valid(url));}
}
