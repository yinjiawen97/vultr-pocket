import com.yjw.vultr.Core;
import java.time.ZoneId;
public class TimeTest {
 public static void main(String[] args){
  if(!Core.time("2026-10-22T00:30:00Z",ZoneId.of("America/Los_Angeles")).equals("2026-10-21 17:30:00 -07:00"))throw new AssertionError("date rollover");
  if(!Core.time("2026-12-22T00:30:00Z",ZoneId.of("America/Los_Angeles")).equals("2026-12-21 16:30:00 -08:00"))throw new AssertionError("winter offset");
  if(!Core.time("2026-10-22T00:30:00+00:00",ZoneId.of("Asia/Shanghai")).equals("2026-10-22 08:30:00 +08:00"))throw new AssertionError("China");
  if(!Core.time("").equals("尚无记录"))throw new AssertionError("missing time");
  if(!Core.time("bad").equals("时间格式未知"))throw new AssertionError("invalid time");
  java.time.Instant now=java.time.Instant.parse("2026-10-24T07:13:00Z");ZoneId sh=ZoneId.of("Asia/Shanghai");
  if(!Core.shortTime("2026-10-24T01:05:00Z",sh,now).equals("09:05"))throw new AssertionError("short today");
  if(!Core.shortTime("2026-10-23T10:05:24Z",sh,now).equals("10-23 18:05"))throw new AssertionError("short this year");
  if(!Core.shortTime("2026-10-23T16:30:00Z",sh,now).equals("00:30"))throw new AssertionError("short local day rollover");
  if(!Core.shortTime("2025-12-01T00:00:00Z",sh,now).equals("2025-12-01"))throw new AssertionError("short older");
  if(!Core.shortTime("",sh,now).equals("尚无记录")||!Core.shortTime("bad",sh,now).equals("时间格式未知"))throw new AssertionError("short invalid");
  System.out.println("PASS: 10 time checks");
 }
}
