import com.yjw.vultr.Adoption;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
public class AdoptionTest {
 static int n=0;
 static void check(boolean v){n++;if(!v)throw new AssertionError("case "+n);}
 static String b64(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
 static boolean rejected(String userData){try{Adoption.credentials(userData);return false;}catch(java.io.IOException e){return e.getMessage().contains("无法取回");}}
 public static void main(String[] args)throws Exception{
  String password="0123456789abcdef0123456789abcdef",token="fedcba9876543210fedcba9876543210";
  // The app's real bootstrap script with a phone-style payload (compact JSON, extra ssh fields).
  String script=new String(Files.readAllBytes(Paths.get("assets/bootstrap.sh")),StandardCharsets.UTF_8);
  String phone="{\"ssh_username\":\"vultr\",\"ssh_password\":\"TestOnlyPassw0rd123\",\"password\":\""+password+"\",\"token\":\""+token+"\",\"yaml\":\"proxies: []\"}";
  String[] got=Adoption.credentials(b64(script.replace("__PAYLOAD__",b64(phone))));check(got[0].equals(password));check(got[1].equals(token));
  // Mac-style payload: Python json.dumps spacing, no ssh fields, key order differs.
  String mac="{\"password\": \""+password+"\", \"token\": \""+token+"\", \"yaml\": \"mixed-port: 7890\\n\"}";
  got=Adoption.credentials(b64("#!/bin/bash\npython3 - <<'SERVER_PY'\npayload = json.loads(base64.b64decode('"+b64(mac)+"'))\nSERVER_PY\n"));check(got[0].equals(password));check(got[1].equals(token));
  // Line-wrapped base64, as some API clients return it.
  String wrapped=Base64.getMimeEncoder().encodeToString(script.replace("__PAYLOAD__",b64(mac)).getBytes(StandardCharsets.UTF_8));check(wrapped.contains("\r\n"));check(Adoption.credentials(wrapped)[1].equals(token));
  for(String bad:new String[]{null,"","  ","not base64 !!!",b64("#!/bin/bash\necho hello\n"),b64("b64decode('"+b64("{\"token\":\""+token+"\"}")+"')"),b64("b64decode('"+b64("{\"password\":\"short\",\"token\":\""+token+"\"}")+"')"),b64("b64decode('"+b64("{\"ssh_password\":\""+password+"\",\"token\":\""+token+"\"}")+"')")})check(rejected(bad));
  String body="proxies:\n- {name: Vultr-Proxy, server: 203.0.113.42, password: "+password+"}\n";
  check(Adoption.matches(body,password,"203.0.113.42"));check(!Adoption.matches(body,token,"203.0.113.42"));check(!Adoption.matches(body,password,"203.0.113.43"));check(!Adoption.matches(null,password,"203.0.113.42"));check(!Adoption.matches("<html>404</html>",password,"203.0.113.42"));
  System.out.println("PASS: "+n+" adoption checks (phone and Mac payloads, wrapped base64, missing or foreign scripts, served config match)");
 }
}
