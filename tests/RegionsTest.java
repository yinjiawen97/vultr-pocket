import com.yjw.vultr.Regions;
import java.time.Instant;
import java.time.ZoneId;
public class RegionsTest {
 static int n=0;
 static void same(String actual,String expected){n++;if(!expected.equals(actual))throw new AssertionError("case "+n+": expected "+expected+" but got "+actual);}
 public static void main(String[] args){
  // Every region Vultr lists today must be known and carry a valid time zone.
  String[] ids={"ams","atl","blr","bom","cdg","del","dfw","ewr","fra","hnl","icn","itm","jnb","lax","lhr","mad","man","mel","mex","mia","mxp","nrt","ord","sao","scl","sea","sgp","sjc","sto","syd","tlv","waw","yto"};
  for(String id:ids){n++;if(Regions.zone(id)==null||Regions.title(id).equals(id.toUpperCase()))throw new AssertionError("unknown region "+id);ZoneId.of(Regions.zone(id));if(Regions.flag(id).codePointCount(0,Regions.flag(id).length())!=2)throw new AssertionError("flag "+id);}
  same(Regions.title("sjc"),"硅谷 · 美国");same(Regions.title(" SJC "),"硅谷 · 美国");same(Regions.title("sgp"),"新加坡");same(Regions.title("nrt"),"东京 · 日本");
  same(Regions.title("xyz"),"XYZ");same(Regions.title(""),"地区待获取");same(Regions.title(null),"地区待获取");
  same(Regions.flag("sjc"),"🇺🇸");same(Regions.flag("nrt"),"🇯🇵");same(Regions.flag("xyz"),"🌐");same(Regions.flag(null),"🌐");
  same(Regions.code(" sjc "),"SJC");same(Regions.code(null),"");
  n++;if(Regions.zone("xyz")!=null||Regions.zone(null)!=null)throw new AssertionError("zone invented for unknown region");
  ZoneId shanghai=ZoneId.of("Asia/Shanghai");Instant winter=Instant.parse("2026-11-10T12:00:00Z"),summer=Instant.parse("2026-07-10T12:00:00Z");
  same(Regions.offset("sjc",shanghai,winter),"比本机慢 16 小时");same(Regions.offset("sjc",shanghai,summer),"比本机慢 15 小时");
  same(Regions.offset("nrt",shanghai,winter),"比本机快 1 小时");same(Regions.offset("sgp",shanghai,winter),"与本机同一时间");
  same(Regions.offset("bom",shanghai,winter),"比本机慢 2 小时 30 分钟");same(Regions.offset("bom",ZoneId.of("Asia/Kathmandu"),winter),"比本机慢 15 分钟");
  same(Regions.offset("syd",shanghai,winter),"比本机快 3 小时");same(Regions.offset("xyz",shanghai,winter),"");
  System.out.println("PASS: "+n+" region checks (all current Vultr regions, names, flags, zones, clock offsets incl. DST and half hours)");
 }
}
