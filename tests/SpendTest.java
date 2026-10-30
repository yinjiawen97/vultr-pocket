import com.yjw.vultr.Spend;
import java.time.Instant;
import org.json.*;
public class SpendTest {
 static int n=0;
 static void check(boolean v){n++;if(!v)throw new AssertionError("case "+n);}
 static JSONObject line(String description,String end,String total)throws Exception{return new JSONObject().put("product","Vultr Cloud Compute").put("description",description).put("end_date",end).put("total",total);}
 public static void main(String[] args)throws Exception{
  JSONObject server=new JSONObject().put("main_ip","203.0.113.42").put("label","demo").put("date_created","2026-10-26T01:40:00+00:00");
  JSONArray items=new JSONArray()
   .put(line("203.0.113.42 (1 vCPU) [demo]","2026-10-30T00:00:00+00:00","0.62"))
   .put(line("203.0.113.42 (1 vCPU) [demo]","2026-10-31T00:00:00+00:00","0.10"))
   .put(line("203.0.113.42 (1 vCPU) [old]","2026-10-30T00:00:00+00:00","9.00"))   // same IP, other server
   .put(line("203.0.113.42 (1 vCPU)","2026-10-20T00:00:00+00:00","5.00"))          // ended before this server existed
   .put(line("203.0.113.4 (1 vCPU)","2026-10-30T00:00:00+00:00","7.00"))           // IP prefix only
   .put(new JSONObject().put("product","Backups").put("description","203.0.113.42 backup").put("end_date","2026-10-30T00:00:00+00:00").put("total","1.00"));
  check(Spend.total(items,server).toPlainString().equals("0.72"));
  check(Spend.total(new JSONArray(),server).signum()==0);
  Instant created=Instant.parse("2026-10-26T01:40:00Z");
  check(Spend.uptime("2026-10-26T01:40:00+00:00",created.plusSeconds(20)).equals("1 分钟"));
  check(Spend.uptime("2026-10-26T01:40:00+00:00",created.plusSeconds(3*3600+25*60)).equals("3 小时 25 分"));
  check(Spend.uptime("2026-10-26T01:40:00+00:00",created.plusSeconds(4*86400+5*3600)).equals("4 天 5 小时"));
  check(Spend.uptime("2026-10-26T01:40:00+00:00",created.minusSeconds(60)).equals("刚创建"));
  check(Spend.uptime("bad",created).equals("时间未知"));
  System.out.println("PASS: "+n+" spend checks");
 }
}
