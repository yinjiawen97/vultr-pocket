import com.yjw.vultr.SubscriptionLinks;
import java.net.URI;
import java.net.URLDecoder;
public class SubscriptionLinksTest {
 public static void main(String[] args)throws Exception{
  String url="https://example.test/sub/token?a=1&b=hello%20world#fragment";
  String fl=SubscriptionLinks.flclash(url);if(!URLDecoder.decode(new URI(fl).getRawQuery().substring(4),"UTF-8").equals(url))throw new AssertionError("nested URL changed");
  String verge=SubscriptionLinks.verge(url,"硅谷 & 节点");String query=new URI(verge).getRawQuery();if(!query.startsWith("name="))throw new AssertionError("name must precede url");if(!URLDecoder.decode(query.substring(query.indexOf("&url=")+5),"UTF-8").equals(url))throw new AssertionError("Verge URL changed");
  for(String value:new String[]{"file:///secret","intent://whatever","javascript:alert(1)","https://user:pass@example.test/","http://","https://example.test/\n"}){boolean invalid=false;try{SubscriptionLinks.valid(value);}catch(IllegalArgumentException e){invalid=true;}if(!invalid)throw new AssertionError("invalid URL accepted");}
  if(SubscriptionLinks.clients(false,false).length!=0||SubscriptionLinks.clients(true,false).length!=1||SubscriptionLinks.clients(false,true).length!=1||SubscriptionLinks.clients(true,true).length!=2)throw new AssertionError("client availability cases");
  if(!SubscriptionLinks.clients(false,true)[0].equals("com.fclash.app")||!SubscriptionLinks.clients(true,true)[0].equals("com.follow.clash"))throw new AssertionError("explicit package routing");
  if(!SubscriptionLinks.clientName("com.follow.clash").contains("官方")||!SubscriptionLinks.clientName("com.fclash.app").contains("SumCheung"))throw new AssertionError("indistinguishable clients");
  try{SubscriptionLinks.clientName("com.unknown.clash");throw new AssertionError("unknown target accepted");}catch(IllegalArgumentException expected){}
  System.out.println("PASS: subscription encoding, Verge parameters, URL validation, both/one/no clients and explicit package allowlist");
 }
}
