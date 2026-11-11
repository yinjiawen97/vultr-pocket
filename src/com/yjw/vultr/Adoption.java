package com.yjw.vultr;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
// Recovers a deployment's subscription credentials from the instance's own startup script (Vultr user-data).
// Both this app and the Mac tool embed the same base64 JSON payload there, so a server deployed elsewhere can be picked up here.
public final class Adoption {
 private Adoption(){}
 static final String MISSING="这台服务器的启动脚本里没有订阅信息，无法取回。只有用本 App 或 Mac 工具部署的服务器可以取回。";
 private static final Pattern PAYLOAD=Pattern.compile("b64decode\\('([A-Za-z0-9+/=]{16,})'\\)");
 private static String field(String json,String name){Matcher m=Pattern.compile("\""+name+"\"\\s*:\\s*\"([0-9a-f]{32,64})\"").matcher(json);return m.find()?m.group(1):null;}
 // {node password, subscription token} from the base64 user-data returned by the cloud.
 public static String[] credentials(String userData)throws IOException{
  if(userData==null||userData.trim().isEmpty())throw new IOException(MISSING);
  try{
   Matcher m=PAYLOAD.matcher(new String(Base64.getMimeDecoder().decode(userData.trim()),StandardCharsets.UTF_8));if(!m.find())throw new IOException(MISSING);
   String json=new String(Base64.getDecoder().decode(m.group(1)),StandardCharsets.UTF_8),password=field(json,"password"),token=field(json,"token");
   if(password==null||token==null)throw new IOException(MISSING);return new String[]{password,token};
  }catch(IllegalArgumentException e){throw new IOException(MISSING);}
 }
 // The served config must be this deployment's: it carries the node password and points at this server.
 public static boolean matches(String body,String password,String ip){return body!=null&&body.contains(password)&&body.contains(ip)&&body.contains("proxies");}
}
