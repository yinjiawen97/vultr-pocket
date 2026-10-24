import com.yjw.vultr.*;
import java.util.*;
import java.nio.file.*;
public class RoutingUpdateTest {
 static void check(boolean v){if(!v)throw new AssertionError("routing update");}
 static class Endpoint implements RoutingUpdate.Endpoint{Map<String,Object> value;int writes;boolean lose,corrupt,rollbackFail;Endpoint(Map<String,Object> v){value=v;}public Map<String,Object> read(){return value;}public void write(Map<String,Object> v)throws Exception{writes++;if(writes>1&&rollbackFail)throw new Exception();value=v;if(writes==1&&corrupt)value=Collections.singletonMap("broken",true);if(writes==1&&lose)throw new Exception();}}
 public static void main(String[] args)throws Exception{
  Map<String,Object> template=Configs.parse(new String(Files.readAllBytes(Paths.get("assets/config.yaml")),"UTF-8"));
  Map<String,Object> old=Configs.parse("proxies: [{name: first, type: ss, server: 203.0.113.1, port: 8388, cipher: chacha20-ietf-poly1305, password: PRIVATE_TEST}, {name: second, type: ss, server: 203.0.113.2, port: 8388, cipher: chacha20-ietf-poly1305, password: OTHER_TEST}]\nrules: [MATCH,DIRECT]\nproxy-groups: []\n");
  Map<String,Object> next=Configs.routing(template,old);check(next.get("proxies").equals(old.get("proxies")));check(((List)next.get("proxy-groups")).size()==8);for(Object g:(List)next.get("proxy-groups")){List gp=(List)((Map)g).get("proxies");String gn=(String)((Map)g).get("name");check(gp.get(0).equals(gn.equals("Apple")?"DIRECT":"first"));check(gn.equals("Claude")!=gp.contains("DIRECT"));}
  check(Configs.parse(Configs.yaml(next)).equals(next));
  List<String> rules=(List)next.get("rules");check(rules.indexOf("RULE-SET,anthropic,Claude")<rules.indexOf("RULE-SET,cn,DIRECT"));check(rules.get(0).equals("DOMAIN-SUFFIX,ts.net,DIRECT"));check(!rules.contains("RULE-SET,anthropic,AI"));check(rules.indexOf("RULE-SET,apple-cn,DIRECT")<rules.indexOf("RULE-SET,apple,Apple"));check(rules.indexOf("RULE-SET,youtube,YouTube")<rules.indexOf("RULE-SET,google,Google"));check(rules.get(rules.size()-1).equals("MATCH,PROXY"));
  Map<String,Object> dns=(Map)next.get("dns");check(dns.get("nameserver").toString().contains("#PROXY"));check(dns.get("proxy-server-nameserver").toString().contains("#DIRECT"));for(Object p:((Map)next.get("rule-providers")).values()){check(((Map)p).get("format").equals("mrs"));check(((Number)((Map)p).get("interval")).intValue()==86400);check(((Map)p).get("proxy").equals("PROXY"));}
  for(int mode=0;mode<4;mode++){Endpoint e=new Endpoint(old);e.lose=mode==1;e.corrupt=mode>=2;e.rollbackFail=mode==3;final boolean[] saved={false};boolean error=false;try{RoutingUpdate.apply(e,b->{check(e.writes==0&&b.equals(old));saved[0]=true;},v->{check(e.writes==0);},next);}catch(Exception ex){error=true;check(!ex.getMessage().contains("PRIVATE_TEST"));}check(saved[0]);if(mode==0)check(!error&&e.value.equals(next));else if(mode<3)check(error&&e.value.equals(old));else check(error);}
  Endpoint untouched=new Endpoint(old);try{RoutingUpdate.apply(untouched,b->{},v->{throw new Exception();},next);throw new AssertionError();}catch(Exception ex){}check(untouched.writes==0);
  String legacy="old REPLACE_SERVER_IP",snapshot="new REPLACE_SERVER_IP";check(Configs.verification("",legacy,"203.0.113.1").equals("old 203.0.113.1"));check(Configs.verification(snapshot,legacy,"203.0.113.2").equals("new 203.0.113.2"));
  System.out.println("PASS: routing node/name preservation, service order, DNS/providers, backup-before-write, rollback/lost response/failure, legacy/snapshot verification");
 }
}
