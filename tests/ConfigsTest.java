import com.yjw.vultr.Configs;
import java.util.*;
public class ConfigsTest {
 public static void main(String[] args)throws Exception{
  String raw="proxies:\n  - name: same\n    type: ss\n    server: 203.0.113.1\n    port: 8388\n    cipher: chacha20-ietf-poly1305\n    password: test-only\n";
  Map<String,Object> a=Configs.parse(raw),b=Configs.parse(raw.replace("203.0.113.1","203.0.113.2"));
  Map<String,Object> base=Configs.parse("rules: [MATCH,PROXY]\n");Map<String,Object> merged=Configs.merge(base,Arrays.asList("日本","美国"),Arrays.asList(a,b));
  List<Map<String,Object>> nodes=Configs.nodes(merged);if(nodes.size()!=2||nodes.get(0).get("name").equals(nodes.get(1).get("name")))throw new AssertionError("merge lost/duplicated names");
  if(!nodes.get(0).get("password").equals("test-only"))throw new AssertionError("credential changed");
  if(((List)merged.get("proxy-groups")).size()!=8)throw new AssertionError("routing groups lost");
  for(String invalid:Arrays.asList("!!javax.script.ScriptEngineManager []","proxies: []","proxy-providers: {}","proxies: [{server: x, type: ss, port: 0}]")){boolean rejected=false;try{Configs.nodes(Configs.parse(invalid));}catch(Exception e){rejected=true;}if(!rejected)throw new AssertionError("invalid/unsafe config accepted");}
  Map<String,Object> test=Configs.testConfig(nodes.get(0),12345);if(!test.get("bind-address").equals("127.0.0.1")||!test.get("allow-lan").equals(false)||!test.get("rules").toString().contains("TARGET"))throw new AssertionError("probe isolation");
  System.out.println("PASS: safe YAML, all-or-nothing merge, unique names, credentials and routing preserved, loopback-only probe");
 }
}
