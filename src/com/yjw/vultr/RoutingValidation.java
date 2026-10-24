package com.yjw.vultr;
import android.content.Context;
import org.json.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
final class RoutingValidation {
 static String reason(File log,Map<String,Object> source){try{String text=new String(Files.readAllBytes(log.toPath()),"UTF-8");for(Map<String,Object> node:Configs.nodes(source))for(String key:Arrays.asList("password","uuid","private-key","public-key")){Object value=node.get(key);if(value instanceof String&&!((String)value).isEmpty())text=text.replace((String)value,"[凭据隐藏]");}text=text.replaceAll("https?://[^\\s\"']+","[地址隐藏]").replaceAll("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}","[IP]");String selected="";for(String line:text.split("\\n"))if(line.contains("error")||line.contains("fatal")||line.contains("panic")||line.contains("denied")||line.contains("not permitted"))selected=line;if(selected.isEmpty())selected="进程未成功退出，请核对系统兼容性";int msg=selected.indexOf("msg=");if(msg>=0)selected=selected.substring(msg+4);return selected.length()>65?selected.substring(0,65):selected;}catch(Exception e){return "无法读取本机验证日志";}}
 static void check(Context context,Map<String,Object> source)throws Exception{
  File binary=new File(context.getApplicationInfo().nativeLibraryDir,"libmihomo.so"),dir=new File(context.getCacheDir(),"routing-"+UUID.randomUUID());if(!binary.isFile()||!dir.mkdir())throw new IOException("缺少本机 Mihomo 验证内核");Process process=null;
  try{
   Map<String,Object> c=new LinkedHashMap<>(source);for(String k:Arrays.asList("tun","listeners","port","socks-port","redir-port","tproxy-port","external-controller-tls","external-controller-unix","external-controller-pipe","external-ui","external-ui-url","external-ui-name","authentication"))c.remove(k);
   int port;try(ServerSocket s=new ServerSocket(0,0,InetAddress.getByName("127.0.0.1"))){port=s.getLocalPort();}
   String secret=UUID.randomUUID().toString();c.put("mixed-port",0);c.put("allow-lan",false);c.put("bind-address","127.0.0.1");c.put("external-controller","127.0.0.1:"+port);c.put("secret",secret);
   File config=new File(dir,"config.json");Files.write(config.toPath(),Configs.yaml(c).getBytes("UTF-8"));
   process=new ProcessBuilder(binary.getAbsolutePath(),"-t","-d",dir.getAbsolutePath(),"-f",config.getAbsolutePath()).redirectErrorStream(true).redirectOutput(new File(dir,"syntax.log")).start();
   if(!process.waitFor(45,java.util.concurrent.TimeUnit.SECONDS)||process.exitValue()!=0)throw new IOException("Mihomo 校验未通过，未发布："+reason(new File(dir,"syntax.log"),source));
   process=new ProcessBuilder(binary.getAbsolutePath(),"-d",dir.getAbsolutePath(),"-f",config.getAbsolutePath()).redirectErrorStream(true).redirectOutput(new File(dir,"runtime.log")).start();
   long until=System.currentTimeMillis()+90000;int expected=((Map)c.get("rule-providers")).size();
   while(System.currentTimeMillis()<until){if(!process.isAlive())break;HttpURLConnection conn=null;try{conn=(HttpURLConnection)new URL("http://127.0.0.1:"+port+"/providers/rules").openConnection();conn.setConnectTimeout(300);conn.setReadTimeout(1000);conn.setRequestProperty("Authorization","Bearer "+secret);JSONObject providers=new JSONObject(Api.read(conn.getInputStream(),1024*1024)).getJSONObject("providers");boolean ok=providers.length()==expected;Iterator<String> names=providers.keys();while(names.hasNext())if(providers.getJSONObject(names.next()).optInt("ruleCount")<=0)ok=false;if(ok)return;}catch(IOException|JSONException e){}finally{if(conn!=null)conn.disconnect();}Thread.sleep(250);}
   throw new IOException("规则首次下载未全部完成，保留原订阅；请检查节点和网络后重试");
  }finally{if(process!=null){process.destroy();if(!process.waitFor(2,java.util.concurrent.TimeUnit.SECONDS))process.destroyForcibly();}NodeProbe.remove(dir);}
 }
}
