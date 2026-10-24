import com.yjw.vultr.BootState;
import org.json.JSONObject;
public class BootStateTest {
 static void eq(String actual,String expected){if(!actual.equals(expected))throw new AssertionError(actual+" != "+expected);}
 public static void main(String[] args)throws Exception{
  eq(BootState.kind("pending","stopped","installingbooting",true),"creating");eq(BootState.kind("active","stopped","installingbooting",true),"creating");eq(BootState.kind("active","running","installingbooting",true),"booting");eq(BootState.kind("active","running","ok",true),"running");eq(BootState.kind("active","stopped","ok",true),"stopped");eq(BootState.kind("active","running","locked",true),"unknown");eq(BootState.kind("active","running","ok",false),"unknown");eq(BootState.kind("active","running","",true),"running");
  JSONObject legacy=new JSONObject().put("cloud","active / running").put("checked_at","2026-10-23T12:00:00Z").put("phase","created");long time=java.time.Instant.parse("2026-10-23T12:00:30Z").toEpochMilli();eq(BootState.record(legacy,time,false),"running");eq(BootState.record(legacy,time,true),"unknown");eq(BootState.record(legacy,time+90000,false),"unknown");eq(BootState.record(legacy,time-60000,false),"unknown");legacy.put("phase","create-pending");eq(BootState.record(legacy,time,false),"uncertain");legacy.put("phase","deleted");eq(BootState.record(legacy,time,false),"deleted");
  legacy.put("phase","created").put("mode","reinstall");legacy.remove("checked_at");eq(BootState.record(legacy,time,false),"unknown");
  if(!BootState.observe("booting","created",true)||!BootState.observe("unknown","created",true)||!BootState.observe("creating","created",true))throw new AssertionError("startup observation missing");
  for(String kind:new String[]{"running","stopped","deleted","uncertain"})if(BootState.observe(kind,"created",true))throw new AssertionError("unnecessary polling "+kind);
  if(BootState.observe("booting","create-pending",true)||BootState.observe("booting","deleted",true)||BootState.observe("booting","created",false))throw new AssertionError("uncertain/deleted/no-ID must not be polled");
  if(!BootState.polling("booting","created",true,179999,true)||BootState.polling("booting","created",true,180000,true)||BootState.polling("booting","created",true,1,false)||BootState.polling("booting","created",true,-1,true))throw new AssertionError("observation timeout/background gates");
  if(BootState.INTERVAL_MS<10000)throw new AssertionError("cloud polling too frequent");
  System.out.println("PASS: cloud boot transitions, legacy records, stale/error gating and read-only observation eligibility");
 }
}
